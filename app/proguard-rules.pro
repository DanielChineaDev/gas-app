# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.bpo.gasapp.** {
    *** Companion;
}
-keepclasseswithmembers class com.bpo.gasapp.** {
    kotlinx.serialization.KSerializer serializer(...);
}
