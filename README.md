# React Native Rust Plugin
Gradle Plugin for compatibility between Rust Code and TypeScript Code for Android. For this plugin, you need to have Cargo and the Android NDK  (Native Development Kit) installed.

## Dependencies
This project uses the following dependencies:
| Name | Author | License |
|-|-|-
| [antlr4](https://github.com/antlr/antlr4) | [Antlr Project](https://github.com/antlr) | [BSD-3 Clause License](https://github.com/antlr/antlr4/blob/dev/LICENSE.txt) |
| [Java Annotations](https://github.com/JetBrains/java-annotations) | [JetBrains](https://github.com/JetBrains) | [Apache-2.0 License](https://github.com/JetBrains/java-annotations/blob/master/LICENSE.txt) |
| [Night Config (Toml)](https://github.com/TheElectronWill/night-config) | [TheElectronWill](https://github.com/TheElectronWill) | [GNU Lesser General Public License 3.0](https://github.com/TheElectronWill/night-config/blob/master/LICENSE) |

## Configuration
You can configure the plugin with the `react-native-rust` section in your `build.gradle`. The following code shows an example (In the `rustBaseFolder` you can create cargo projects):
```groovy
react-native-rust {
	basePackage = "com.user.example"
	rustBaseFolder = "src/main/rust"
	androidApiVersion = 33 as Byte
	
	cargoFile = file("%CARGO_PATH%") // Optional, defaulted to cargo executable
	ndkFolder = file("%NDK_FOLDER%") // Optional, defaulted to NDK_HOME env variable

	module('rust-module-name') // Add module to list
}
```

After the configuration, you can run three tasks:
- `javaCodeGen` - Generate Java-side Code by the Rust code in the projects
- `cargoCompile` - Compile all Rust projects in base folder
- `nativeBundle` - Move all rust library files to the `src/main/jniLibs` folder
