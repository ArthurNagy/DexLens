#!/usr/bin/env python3
"""Builds DexLens's prebuilt Room database from PokeAPI's CSV tables.

    python3 tools/build_database.py

The tables come from Room's exported schema, so the file always passes Room's validation. PokeAPI is fetched
at a pinned commit into the git-ignored tools/.cache/ folder, rows are inserted in a stable order and the file
is vacuumed, so two runs on one machine produce the same bytes.

Standard library only. PokeAPI's data is BSD-3-Clause: https://github.com/PokeAPI/pokeapi
"""

from __future__ import annotations

import argparse
import collections
import csv
import hashlib
import json
import re
import sqlite3
import subprocess
import sys
import unicodedata
from pathlib import Path

POKEAPI_URL = "https://github.com/PokeAPI/pokeapi"
SOURCE_COMMIT = "5cbeb3f3eb55a9b86da52c4869336f9d61fdd6d7"
CSV_SUBDIR = "data/v2/csv"

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_CACHE = ROOT / "tools/.cache/pokeapi"
DEFAULT_SCHEMA = ROOT / "core/database/schemas/com.arthurnagy.dexlens.core.database.DexDatabase/1.json"
DEFAULT_OUTPUT = ROOT / "core/database/src/main/assets/databases/dexlens.db"

ENGLISH = 9
SPECIES_COUNT = 1025
TYPE_COUNT = 18
EXPECTED_NAME_LANGUAGES = 12
STAT_COLUMNS = {
    "1": "hp",
    "2": "attack",
    "3": "defense",
    "4": "special_attack",
    "5": "special_defense",
    "6": "speed",
}
TRICKY_NAMES = {
    29: "Nidoran♀",
    32: "Nidoran♂",
    83: "Farfetch’d",
    122: "Mr. Mime",
    250: "Ho-Oh",
    474: "Porygon-Z",
    669: "Flabébé",
    772: "Type: Null",
    1025: "Pecharunt",
}

Row = dict[str, str]


def fetch_pokeapi(cache: Path) -> Path:
    """Sparse, shallow checkout of the pinned commit. Reuses an existing checkout of that commit."""
    git = ["git", "-C", str(cache)]
    head = subprocess.run([*git, "rev-parse", "HEAD"], capture_output=True, text=True, check=False)
    if not (cache / CSV_SUBDIR).is_dir() or head.stdout.strip() != SOURCE_COMMIT:
        cache.mkdir(parents=True, exist_ok=True)
        subprocess.run([*git, "init", "-q"], check=True)
        subprocess.run([*git, "remote", "remove", "origin"], capture_output=True, check=False)
        subprocess.run([*git, "remote", "add", "origin", POKEAPI_URL], check=True)
        subprocess.run([*git, "sparse-checkout", "set", CSV_SUBDIR], check=True)
        subprocess.run([*git, "fetch", "-q", "--depth", "1", "--filter=blob:none", "origin", SOURCE_COMMIT], check=True)
        subprocess.run([*git, "checkout", "-q", "--detach", "FETCH_HEAD"], check=True)
    return cache / CSV_SUBDIR


def rows(csv_dir: Path, name: str) -> list[Row]:
    with (csv_dir / f"{name}.csv").open(encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle))


def english(csv_dir: Path, table: str, key: str, column: str = "name") -> dict[str, str]:
    return {row[key]: row[column] for row in rows(csv_dir, table) if int(row["local_language_id"]) == ENGLISH and row[column]}


def to_int(value: str) -> int | None:
    return int(value) if value else None


def is_latin(char: str) -> bool:
    return "LATIN" in unicodedata.name(char, "")


def normalize(name: str) -> list[str]:
    """Search tokens for a name. Must stay in step with Kotlin's SpeciesNameNormalizer."""
    text = name.replace("♀", " f ").replace("♂", " m ").lower()
    folded: list[str] = []
    for char in unicodedata.normalize("NFD", text):
        if unicodedata.category(char) == "Mn" and folded and is_latin(folded[-1]):
            continue
        folded.append(char)
    return re.findall(r"[^\W_]+", unicodedata.normalize("NFC", "".join(folded)))


def search_text(name: str) -> str:
    tokens = normalize(name)
    compact = "".join(tokens)
    return " ".join(tokens if len(tokens) == 1 else [*tokens, compact])


def title_case(identifier: str) -> str:
    return " ".join(part.capitalize() for part in identifier.replace("_", "-").split("-"))


def sentence_label(name: str) -> str:
    """PokeAPI spells some language names in lower case, such as "Simplified chinese"."""
    return " ".join(word if word == "and" else word[0].upper() + word[1:] for word in name.split())


MARKUP = re.compile(r"\[([^\]]*)\]\{(\w+):([^}]+)\}")


def clean_prose(text: str | None) -> str | None:
    """Turns PokeAPI's link markup into plain words: `[flinch]{mechanic:flinch}` and `[]{type:fire}`."""
    if not text:
        return None

    def replace(match: re.Match[str]) -> str:
        label, _kind, target = match.groups()
        return label or title_case(target)

    return MARKUP.sub(replace, text).replace("$effect_chance", "the effect chance").strip()


class Builder:
    def __init__(self, csv_dir: Path, schema_path: Path, output: Path) -> None:
        self.csv = csv_dir
        self.schema = json.loads(schema_path.read_text(encoding="utf-8"))["database"]
        self.output = output
        output.parent.mkdir(parents=True, exist_ok=True)
        output.unlink(missing_ok=True)
        self.db = sqlite3.connect(output)
        self.db.execute("PRAGMA page_size = 4096")
        self.db.execute("PRAGMA journal_mode = DELETE")
        self.columns: dict[str, list[str]] = {}

    def create_tables(self) -> None:
        for entity in self.schema["entities"]:
            table = entity["tableName"]
            self.db.execute(entity["createSql"].replace("${TABLE_NAME}", table))
            for index in entity.get("indices", []):
                self.db.execute(index["createSql"].replace("${TABLE_NAME}", table))
            for trigger in entity.get("contentSyncTriggers", []):
                self.db.execute(trigger.replace("${TABLE_NAME}", table))
            self.columns[table] = [field["columnName"] for field in entity["fields"]]
        for query in self.schema["setupQueries"]:
            self.db.execute(query)
        self.db.execute(f"PRAGMA user_version = {int(self.schema['version'])}")

    def insert(self, table: str, records: list[dict[str, object]]) -> None:
        columns = self.columns[table]
        placeholders = ",".join("?" for _ in columns)
        names = ",".join(f"`{column}`" for column in columns)
        statement = f"INSERT INTO `{table}` ({names}) VALUES ({placeholders})"
        self.db.executemany(statement, [tuple(record[column] for column in columns) for record in records])

    def build(self) -> None:
        self.create_tables()
        species = self.species()
        self.names(species)
        self.languages()
        varieties = self.varieties(species)
        self.forms(varieties)
        self.abilities(varieties)
        self.egg_groups(species)
        self.evolutions(species)
        self.type_chart()
        self.game_versions()
        self.search_index()
        self.db.commit()
        self.db.execute("INSERT INTO species_name_fts(species_name_fts) VALUES('optimize')")
        self.db.commit()
        self.db.execute("VACUUM")
        self.validate()
        self.db.close()

    def species(self) -> set[int]:
        records = [
            {
                "id": int(row["id"]),
                "identifier": row["identifier"],
                "generation": int(row["generation_id"]),
                "gender_rate": int(row["gender_rate"]),
                "evolves_from_species_id": to_int(row["evolves_from_species_id"]),
                "evolution_chain_id": int(row["evolution_chain_id"]),
                "is_baby": int(row["is_baby"]),
                "is_legendary": int(row["is_legendary"]),
                "is_mythical": int(row["is_mythical"]),
            }
            for row in rows(self.csv, "pokemon_species")
            if int(row["id"]) <= SPECIES_COUNT
        ]
        records.sort(key=lambda record: record["id"])
        self.insert("species", records)
        return {int(record["id"]) for record in records}

    def names(self, species: set[int]) -> None:
        records = [
            {
                "species_id": int(row["pokemon_species_id"]),
                "language_id": int(row["local_language_id"]),
                "name": row["name"],
                "genus": row["genus"] or None,
            }
            for row in rows(self.csv, "pokemon_species_names")
            if int(row["pokemon_species_id"]) in species and row["name"]
        ]
        records.sort(key=lambda record: (record["species_id"], record["language_id"]))
        self.insert("species_name", records)

    def languages(self) -> None:
        used = {row[0] for row in self.db.execute("SELECT DISTINCT language_id FROM species_name")}
        labels = {
            row["language_id"]: row["name"]
            for row in rows(self.csv, "language_names")
            if int(row["local_language_id"]) == ENGLISH
        }
        records = [
            {"id": int(row["id"]), "identifier": row["identifier"], "name": sentence_label(labels.get(row["id"], title_case(row["identifier"])))}
            for row in rows(self.csv, "languages")
            if int(row["id"]) in used
        ]
        records.sort(key=lambda record: record["id"])
        self.insert("language", records)

    def varieties(self, species: set[int]) -> set[int]:
        types: dict[int, dict[int, int]] = collections.defaultdict(dict)
        for row in rows(self.csv, "pokemon_types"):
            types[int(row["pokemon_id"])][int(row["slot"])] = int(row["type_id"])
        stats: dict[int, dict[str, int]] = collections.defaultdict(dict)
        for row in rows(self.csv, "pokemon_stats"):
            if row["stat_id"] in STAT_COLUMNS:
                stats[int(row["pokemon_id"])][STAT_COLUMNS[row["stat_id"]]] = int(row["base_stat"])

        default_form = {row["pokemon_id"]: row["id"] for row in rows(self.csv, "pokemon_forms") if row["is_default"] == "1"}
        form_labels = english(self.csv, "pokemon_form_names", "pokemon_form_id", "pokemon_name")
        species_labels = {
            int(row["pokemon_species_id"]): row["name"]
            for row in rows(self.csv, "pokemon_species_names")
            if int(row["local_language_id"]) == ENGLISH
        }

        records = []
        for row in rows(self.csv, "pokemon"):
            species_id = int(row["species_id"])
            if species_id not in species:
                continue
            variety_id = int(row["id"])
            is_default = row["is_default"] == "1"
            name = species_labels[species_id] if is_default else form_labels.get(default_form.get(row["id"], ""), title_case(row["identifier"]))
            stat_values = stats[variety_id]
            if set(stat_values) != set(STAT_COLUMNS.values()) or 1 not in types[variety_id]:
                raise SystemExit(f"incomplete variety {row['identifier']}")
            records.append(
                {
                    "id": variety_id,
                    "species_id": species_id,
                    "identifier": row["identifier"],
                    "name": name,
                    "is_default": int(is_default),
                    "sort_order": int(row["order"] or variety_id),
                    "height": int(row["height"]),
                    "weight": int(row["weight"]),
                    "type_primary": types[variety_id][1],
                    "type_secondary": types[variety_id].get(2),
                    **stat_values,
                },
            )
        records.sort(key=lambda record: record["id"])
        self.insert("variety", records)
        return {int(record["id"]) for record in records}

    def forms(self, varieties: set[int]) -> None:
        names = english(self.csv, "pokemon_form_names", "pokemon_form_id", "form_name")
        types: dict[int, dict[int, int]] = collections.defaultdict(dict)
        for row in rows(self.csv, "pokemon_form_types"):
            types[int(row["pokemon_form_id"])][int(row["slot"])] = int(row["type_id"])
        records = []
        for row in rows(self.csv, "pokemon_forms"):
            variety_id = int(row["pokemon_id"])
            if variety_id not in varieties:
                continue
            form_id = int(row["id"])
            label = names.get(row["id"]) or title_case(row["form_identifier"] or row["identifier"])
            records.append(
                {
                    "id": form_id,
                    "variety_id": variety_id,
                    "identifier": row["identifier"],
                    "name": label,
                    "is_default": int(row["is_default"]),
                    "is_battle_only": int(row["is_battle_only"]),
                    "is_mega": int(row["is_mega"]),
                    "sort_order": int(row["order"] or form_id),
                    "type_primary": types[form_id].get(1),
                    "type_secondary": types[form_id].get(2),
                },
            )
        records.sort(key=lambda record: record["id"])
        self.insert("form", records)

    def abilities(self, varieties: set[int]) -> None:
        links = [row for row in rows(self.csv, "pokemon_abilities") if int(row["pokemon_id"]) in varieties]
        used = {row["ability_id"] for row in links}
        names = english(self.csv, "ability_names", "ability_id")
        prose = {row["ability_id"]: row for row in rows(self.csv, "ability_prose") if int(row["local_language_id"]) == ENGLISH}
        identifiers = {row["id"]: row["identifier"] for row in rows(self.csv, "abilities")}
        records = [
            {
                "id": int(ability),
                "identifier": identifiers[ability],
                "name": names.get(ability, title_case(identifiers[ability])),
                "short_effect": clean_prose(prose.get(ability, {}).get("short_effect")),
                "effect": clean_prose(prose.get(ability, {}).get("effect")),
            }
            for ability in sorted(used, key=int)
        ]
        self.insert("ability", records)
        link_records = [
            {
                "variety_id": int(row["pokemon_id"]),
                "slot": int(row["slot"]),
                "ability_id": int(row["ability_id"]),
                "is_hidden": int(row["is_hidden"]),
            }
            for row in links
        ]
        link_records.sort(key=lambda record: (record["variety_id"], record["slot"]))
        self.insert("variety_ability", link_records)

    def egg_groups(self, species: set[int]) -> None:
        names = english(self.csv, "egg_group_prose", "egg_group_id")
        records = [
            {"id": int(row["id"]), "identifier": row["identifier"], "name": names.get(row["id"], title_case(row["identifier"]))}
            for row in rows(self.csv, "egg_groups")
        ]
        records.sort(key=lambda record: record["id"])
        self.insert("egg_group", records)
        links = [
            {"species_id": int(row["species_id"]), "egg_group_id": int(row["egg_group_id"])}
            for row in rows(self.csv, "pokemon_egg_groups")
            if int(row["species_id"]) in species
        ]
        links.sort(key=lambda record: (record["species_id"], record["egg_group_id"]))
        self.insert("species_egg_group", links)

    def evolutions(self, species: set[int]) -> None:
        items = english(self.csv, "item_names", "item_id")
        moves = english(self.csv, "move_names", "move_id")
        locations = english(self.csv, "location_names", "location_id")
        regions = english(self.csv, "region_names", "region_id")
        triggers = {row["id"]: row["identifier"] for row in rows(self.csv, "evolution_triggers")}
        parents = {int(row["id"]): to_int(row["evolves_from_species_id"]) for row in rows(self.csv, "pokemon_species")}

        grouped: dict[int, list[Row]] = collections.defaultdict(list)
        for row in rows(self.csv, "pokemon_evolution"):
            grouped[int(row["evolved_species_id"])].append(row)

        records: list[dict[str, object]] = []
        for species_id in sorted(grouped):
            if species_id not in species:
                continue
            parent = parents.get(species_id)
            if parent is None:
                continue
            candidates = grouped[species_id]
            defaults = [row for row in candidates if row["is_default"] == "1"]
            if defaults:
                chosen = defaults
            else:
                latest = max(int(row["version_group_id"]) for row in candidates)
                chosen = [row for row in candidates if int(row["version_group_id"]) == latest]
            seen: set[tuple[object, ...]] = set()
            for row in sorted(chosen, key=lambda item: int(item["id"])):
                record = self.evolution_record(row, species_id, parent, triggers, items, moves, locations, regions)
                signature = tuple(value for key, value in record.items() if key != "id")
                if signature not in seen:
                    seen.add(signature)
                    records.append(record)
        self.insert("evolution", records)

    @staticmethod
    def evolution_record(
        row: Row,
        species_id: int,
        parent: int,
        triggers: dict[str, str],
        items: dict[str, str],
        moves: dict[str, str],
        locations: dict[str, str],
        regions: dict[str, str],
    ) -> dict[str, object]:
        return {
            "id": int(row["id"]),
            "species_id": species_id,
            "from_species_id": parent,
            "trigger": triggers[row["evolution_trigger_id"]],
            "min_level": to_int(row["minimum_level"]),
            "item": items.get(row["trigger_item_id"]),
            "held_item": items.get(row["held_item_id"]),
            # Alcremie has one row per spin direction and time of day. Those decide its appearance, not whether it evolves.
            "time_of_day": None if triggers[row["evolution_trigger_id"]] == "spin" else row["time_of_day"] or None,
            "known_move": moves.get(row["known_move_id"]),
            "known_move_type": to_int(row["known_move_type_id"]),
            "min_happiness": to_int(row["minimum_happiness"]),
            "min_beauty": to_int(row["minimum_beauty"]),
            "min_affection": to_int(row["minimum_affection"]),
            "relative_physical_stats": to_int(row["relative_physical_stats"]),
            "party_species_id": to_int(row["party_species_id"]),
            "party_type": to_int(row["party_type_id"]),
            "trade_species_id": to_int(row["trade_species_id"]),
            "location": locations.get(row["location_id"]),
            "region": regions.get(row["region_id"]),
            "gender": to_int(row["gender_id"]),
            "needs_rain": int(row["needs_overworld_rain"] or 0),
            "turn_upside_down": int(row["turn_upside_down"] or 0),
            "used_move": moves.get(row["used_move_id"]),
            "min_move_count": to_int(row["minimum_move_count"]),
            "min_steps": to_int(row["minimum_steps"]),
            "min_damage_taken": to_int(row["minimum_damage_taken"]),
        }

    def type_chart(self) -> None:
        records = [
            {
                "attacking_type": int(row["damage_type_id"]),
                "defending_type": int(row["target_type_id"]),
                "factor": int(row["damage_factor"]),
            }
            for row in rows(self.csv, "type_efficacy")
            if int(row["damage_type_id"]) <= TYPE_COUNT and int(row["target_type_id"]) <= TYPE_COUNT
        ]
        records.sort(key=lambda record: (record["attacking_type"], record["defending_type"]))
        self.insert("type_efficacy", records)

    def game_versions(self) -> None:
        names = english(self.csv, "version_names", "version_id")
        records = [{"id": int(row["id"]), "name": names.get(row["id"], title_case(row["identifier"]))} for row in rows(self.csv, "versions")]
        records.sort(key=lambda record: record["id"])
        self.insert("game_version", records)

    def search_index(self) -> None:
        names = self.db.execute("SELECT species_id, language_id, name FROM species_name ORDER BY species_id, language_id").fetchall()
        self.insert(
            "species_name_fts",
            [
                {"rowid": index, "species_id": species_id, "language_id": language_id, "search": search_text(name)}
                for index, (species_id, language_id, name) in enumerate(names, start=1)
            ],
        )

    def scalar(self, query: str, *args: object) -> int:
        return self.db.execute(query, args).fetchone()[0]

    def validate(self) -> None:
        checks: dict[str, tuple[int, int]] = {
            "species": (self.scalar("SELECT COUNT(*) FROM species"), SPECIES_COUNT),
            "default varieties": (self.scalar("SELECT COUNT(*) FROM variety WHERE is_default = 1"), SPECIES_COUNT),
            "species with a default variety": (self.scalar("SELECT COUNT(DISTINCT species_id) FROM variety WHERE is_default = 1"), SPECIES_COUNT),
            "name languages": (self.scalar("SELECT COUNT(*) FROM language"), EXPECTED_NAME_LANGUAGES),
            "names": (self.scalar("SELECT COUNT(*) FROM species_name"), SPECIES_COUNT * EXPECTED_NAME_LANGUAGES),
            "fts rows": (self.scalar("SELECT COUNT(*) FROM species_name_fts"), SPECIES_COUNT * EXPECTED_NAME_LANGUAGES),
            "type chart": (self.scalar("SELECT COUNT(*) FROM type_efficacy"), TYPE_COUNT * TYPE_COUNT),
            "default varieties without ability": (
                self.scalar("SELECT COUNT(*) FROM variety v WHERE v.is_default = 1 AND NOT EXISTS (SELECT 1 FROM variety_ability a WHERE a.variety_id = v.id)"),
                0,
            ),
            "species without egg group": (
                self.scalar("SELECT COUNT(*) FROM species s WHERE NOT EXISTS (SELECT 1 FROM species_egg_group g WHERE g.species_id = s.id)"),
                0,
            ),
            "evolved species without evolution": (
                self.scalar(
                    "SELECT COUNT(*) FROM species s WHERE s.evolves_from_species_id IS NOT NULL "
                    "AND NOT EXISTS (SELECT 1 FROM evolution e WHERE e.species_id = s.id)",
                ),
                0,
            ),
        }
        failed = {name: value for name, value in checks.items() if value[0] != value[1]}
        if failed:
            raise SystemExit(f"validation failed (actual, expected): {failed}")
        for species_id, expected in TRICKY_NAMES.items():
            actual = self.db.execute("SELECT name FROM species_name WHERE species_id = ? AND language_id = ?", (species_id, ENGLISH)).fetchone()[0]
            if actual != expected:
                raise SystemExit(f"tricky name {species_id}: {actual!r} != {expected!r}")
        for query, expected_id in [("flabebe*", 669), ('"mr"* "mime"*', 122), ("farfetchd*", 83), ('"type"* "null"*', 772), ("hooh*", 250), ("porygonz*", 474)]:
            ids = {row[0] for row in self.db.execute("SELECT species_id FROM species_name_fts WHERE species_name_fts MATCH ?", (query,))}
            if expected_id not in ids:
                raise SystemExit(f"search {query!r} did not find {expected_id}: {ids}")
        print("validated: " + ", ".join(f"{name}={value[0]}" for name, value in checks.items()))
        missing_prose = self.scalar("SELECT COUNT(*) FROM ability WHERE short_effect IS NULL")
        print(f"abilities without PokeAPI prose: {missing_prose}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--cache", type=Path, default=DEFAULT_CACHE, help="folder for the PokeAPI checkout")
    parser.add_argument("--schema", type=Path, default=DEFAULT_SCHEMA, help="Room's exported schema JSON")
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT, help="database file to write")
    args = parser.parse_args()

    csv_dir = fetch_pokeapi(args.cache)
    try:
        Builder(csv_dir, args.schema, args.output).build()
    except SystemExit:
        args.output.unlink(missing_ok=True)  # A file that failed validation must never be committed.
        raise
    size = args.output.stat().st_size
    digest = hashlib.sha256(args.output.read_bytes()).hexdigest()
    print(f"wrote {args.output.relative_to(ROOT) if args.output.is_relative_to(ROOT) else args.output}: {size / 1048576:.2f} MB, sha256 {digest}")


if __name__ == "__main__":
    sys.exit(main())
