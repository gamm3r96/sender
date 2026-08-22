sed -i 's/val colorScheme = when {/val targetColorScheme = when {/g' app/src/main/java/com/example/ui/theme/Theme.kt
sed -i 's/    val view = LocalView.current/    val colorScheme = animateColorSchemeAsState(targetColorScheme)\n\n    val view = LocalView.current/g' app/src/main/java/com/example/ui/theme/Theme.kt
