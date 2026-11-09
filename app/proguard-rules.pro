# R8 keep rules for :app. Hilt and kotlinx-serialization ship consumer rules; Navigation 3 (1.2.1) ships
# none, and needs none: back-stack saving looks up each @Serializable key's serializer by reflection
# (INSTANCE / Companion + serializer()), which kotlinx-serialization's rules keep. Add rules here only for
# reflection the app itself does, or that a library does without shipping a consumer rule. Audit with the r8-analyzer skill.

# ML Kit finds its components from manifest metadata by class name, which R8 cannot see. Without these the
# registrars are renamed or removed, and TextRecognition.getClient throws a NullPointerException in release builds.
-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }
