.PHONY: clean
clean:
	clj -T:build clean

.PHONY: build
build:
	clj -T:build uber

.PHONY: build-dmg
build-dmg: build
	jpackage \
		--name "Brew Updater" \
		--icon images/icon.icns \
		--type dmg \
		--input target \
		--dest target \
		--main-jar app.jar \
		--main-class brew_updater.core

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
