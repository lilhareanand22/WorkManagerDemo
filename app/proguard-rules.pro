# Add project specific ProGuard rules here.
# WorkManager specific keep rules — prevents R8 from stripping worker classes
# that are referenced only by string in WorkManager's internal database.

# Keep all Worker subclasses so WorkManager can instantiate them by name
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.RxWorker { *; }

# Keep WorkerParameters constructor for WorkManager's default factory
-keep class androidx.work.WorkerParameters { *; }
