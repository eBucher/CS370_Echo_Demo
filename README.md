<p align="center">
    <img height="150px" src="https://raw.githubusercontent.com/eBucher/CS370_Echo_Demo/master/logo.png">
	
</p>

## Features
* The user can ask their Amazon echo about what will be happening on the Sonoma State campus during a certain timeframe such as today, this week, next week, etc.
* The skill can answer questions about specific events such as how much an event costs to attend, where it will take place, etc.
* Questions can be asked about what day something is happening on or how many days there are until that event.
* The skill is automatically synced with the [Sonoma State events calendar](https://www.sonoma.edu/calendar).

### Sample Utterances
> Alexa, ask the Sonoma State calendar what is happening next week on campus.

> Alexa, ask the Sonoma State calendar how much the basketball game tonight costs.

> Alexa, ask the Sonoma State calendar when is spring break.

> Alexa, ask the Sonoma State calendar if there is school on Columbus day.

> Alexa, ask the Sonoma State calendar how many days there are until the end of the semester.

## Technology and languages used
<table><tr><td>Frontend</td><td>Java</td></tr><tr><td>Database</td><td>PostgreSQL</td></tr><tr><td>Scraper for populating database</td><td>Python </td></tr><tr><td>Build tools</td><td>Maven and Gradle (both are supported) </td></tr><tr><td>Hosting</td><td>Amazon Web Services (Lambda and RDS) </td></tr></table>

## Building and deployment to AWS
Complete instructions can be found in the [wiki of the original repository](https://github.com/370-Alexa-Project/CS370_Echo_Demo/wiki/Instructions-for-running-an-AWS-Skill) for this project.

## Authors
* Erich Bucher
* Jorge Canchola
* Scott Cordor
* Jessica Devincenzi
* Tyler Gearing
* Ryan Moeller
* Sarah Mosley
* Aaron Pineda

[![Build Status](https://travis-ci.org/370-Alexa-Project/CS370_Echo_Demo.svg?branch=ssu-calendar)](https://travis-ci.org/370-Alexa-Project/CS370_Echo_Demo)
