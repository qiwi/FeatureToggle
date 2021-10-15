-keepattributes *Annotation*
-keepattributes Signature

-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }

-keepnames class com.fasterxml.jackson.** { *; }
-keepnames class org.codehaus.jackson.** { *; }
-dontwarn com.fasterxml.jackson.databind.**
