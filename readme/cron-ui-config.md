## Cron UI - initial configuration

Cron UI scheduler is based on the project:
https://github.com/alseambusher/crontab-ui

and its docker image published on dockerhub at:
https://hub.docker.com/r/alseambusher/crontab-ui/tags

With Crontab UI, it is very easy and also less error prone to manage crontab. 

Here are the key features of Crontab UI:
1. Easy setup. You can even import from existing crontab.
2. Safe adding, deleting or pausing jobs. Easy to maintain hundreds of jobs.
3. Backup your crontabs.
4. Export crontab and deploy on other machines without much hassle.
5. Error log support.
6. Mailing and hooks support.

In the docker-compose configuration, the crontab ui container is set to load at start a set of 2 cron jobs, 1 for each
node, which re-run the charging process on the first day of each month.

Scheduler saved configuration     |  Scheduler container initial configuration
:--------------------------------:|:-------------------------:
![](/readme/scheduler-saved-configuration.PNG "scheduler saved configuration") |  ![](/readme/scheduler-config.PNG "scheduler container initial configuration")

![Cron UI starts preconfigured with 2 monthly jobs](/readme/scheduler-2-initial-jobs.PNG "Cron UI starts preconfigured with 2 monthly jobs")