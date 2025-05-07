.PHONY: clean
clean:
	clj -T:build clean

.PHONY: build
build:
	clj -T:build uber

# Requires JDK 21 because of the jlink options
.PHONY: build-dmg
build-dmg: build
	mkdir -p target/dmg
	cp target/app.jar target/dmg
	jpackage \
		--name "Brew Updater" \
		--icon images/icon.icns \
		--type dmg \
		--input target/dmg \
		--dest target \
		--main-jar app.jar \
		--main-class brew_updater.core \
		--jlink-options "--strip-native-commands --strip-debug --no-man-pages --no-header-files --compress zip-9" \
		--add-modules "java.base,java.desktop,java.sql,jdk.unsupported"

.PHONY: jdeps
jdeps:
	jdeps \
		--print-module-deps \
		--ignore-missing-deps \
		--module-path /Users/rockjam/.m2/repository/io/github/humbleui/types/0.2.0/types-0.2.0.jar \
		--class-path target \
		--multi-release 21 \
		target/app.jar

.PHONY: package
package: build
	cp images/icon.icns target
	cp scripts/homebrew-updater.sh target

.PHONY: repl
repl:
	clj -M:nrepl

.PHONY: run
run:
	clj -M:run

.PHONY: downgrade-casks
downgrade-casks:
	brew install -s old-casks/signal.rb
