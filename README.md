# IntelliJ IDEA Obsolete Plugins

Over the years, IntelliJ IDEA has accumulated support for a large array of technologies, and many of those technologies are no longer actively maintained. We know that there are still people using those technologies, and up until now we’ve been maintaining plugins for them as part of the main IntelliJ IDEA source repository. However, our project has been growing, and carrying this baggage is getting more and more difficult both for our users (for whom the plugins affect the size of installation and potentially performance) and for our development team. At the same time, we’ve established procedures for maintaining a stable third-party plugin API, so we’re confident that moving plugins out of the main repository will not affect their stability as the IDE evolves.

Starting with IntelliJ IDEA 2019.2, we've moved a number of plugins out of the main IntelliJ IDEA source repository to this repository. Those plugins will no longer be bundled with IntelliJ IDEA, and we will no longer be updating those plugins together with IntelliJ IDEA releases. However, you will still be able to download the plugins from the plugin manager, and we’ll still fix critical issues in those plugins. You’re welcome to submit pull requests with fixes and improvements, and if you do, we will release updates of the plugins with those changes.

This repository includes the following plugins:
  * [J2ME](https://plugins.jetbrains.com/plugin/12318-j2me)
