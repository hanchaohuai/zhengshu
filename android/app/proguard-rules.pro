-addconfigurationwarnings
-dontwarn org.tensorflow.lite.**
-keep class org.tensorflow.lite.** { *; }
-keep class com.zhengshu.** { *; }
-keepclassmembers class * {
    @androidx.room.Entity public *;
}
-keepclassmembers class * {
    @androidx.room.Dao public *;
}
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn javax.annotation.**
