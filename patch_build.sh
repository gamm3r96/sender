sed -i 's/val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}\/my-upload-key.jks"/val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}\/debug.keystore"/g' app/build.gradle.kts
sed -i 's/storePassword = System.getenv("STORE_PASSWORD")/storePassword = System.getenv("STORE_PASSWORD") ?: "android"/g' app/build.gradle.kts
sed -i 's/keyAlias = "upload"/keyAlias = "androiddebugkey"/g' app/build.gradle.kts
sed -i 's/keyPassword = System.getenv("KEY_PASSWORD")/keyPassword = System.getenv("KEY_PASSWORD") ?: "android"/g' app/build.gradle.kts
