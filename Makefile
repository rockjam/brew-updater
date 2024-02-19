.PHONY: clean
clean:
	clj -T:build clean

.PHONY: build
build:
	clj -T:build uber

.PHONY: package
package: build
	cp images/icon.icns target
	cp scripts/homebrew-updater.sh target
