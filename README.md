# Brew Updater

## TODO
* Handle errors during the update
* Update all casks. probably can spawn a lot of threads, and listen to their updates
* Figure out how to respond to password prompts from brew
* Make fonts and paddings prettier

## How to check for updates from brew
1. Check updates with `brew update` 
2. List all folders in `/opt/homebrew/Caskroom` directory - those are installed Casks. +
3. Check current versions of Casks. It's encoded as a name of the folder inside of each cask folder. Or check it using `brew info --cask --json=v2 docker`
4. Make a request to brew API to get the latest version of the cask. curl -s https://formulae.brew.sh/api/cask/evernote.json. Or via `brew info --cask --json=v2 docker`
5. Display casks, outdated first, with buttons to update individual casks, and button to update all casks. 


?. Add Menu Bar icon to the app to show when there is an update available. https://stackoverflow.com/questions/53126884/how-to-have-a-menu-on-the-mac-top-toolbar-with-javafx

https://alvinalexander.com/blog/post/jfc-swing/how-put-java-application-name-mac-menu-bar-menubar/
