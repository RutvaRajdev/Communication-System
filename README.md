# Team Details

* **Team number** : 103
* **Members** :
  * Rutva Rajdev
  * Rohan Chitnis
  * Varad Choudhari
  * Saurabh Singh

# Important Links

* **Live System** : IP: 18.218.137.145, Port: 4545
* **System Setup** : [System setup](https://www.youtube.com/watch?v=wk5zJwLVx-U)
* **Final Presentation** : [Final Presentation](https://www.youtube.com/watch?v=6is0bGAIbmc&feature=youtu.be)

# Project Goal
The main goal of the project was to extend the basic communications server provided as the part of the legacy code. As a part of the project resources, the team was provided with a basic client and server and the goal was to expand them and enhance their functionalities. The client and server provided at this point of time only had the basic capabilities. The client could send a message which would be received by the server. The server would then broadcast this message to all the active clients. The project goal was to enhance these server capabilities in order to make it more robust and to support more advanced communication.

# Our Workflow

Below is our workflow that the team followed :

<a href="https://imgbb.com/"><img src="https://i.ibb.co/5RwPv8L/gitworkflow.png" alt="gitworkflow" border="0"></a>

# How to run the code

Below steps assume that you have a linux debian system. Follow below steps to run the code:

* **Step 1** : Install java

```
$ sudo apt-get update
$ sudo apt-get install default-jre
$ sudo apt-get install default-jdk
```

* **Step 2** : Install maven :

```
$ sudo apt install maven
```

Verify the installation by running the `mvn -version` command

* **Step 3** : Download the github repository

```
$ git clone https://github.ccs.neu.edu/cs5500/team-103-F18.git
$ cd team-103-F18/Project_src
```

* **Step 4** : Open the two projects `Prattle` & `Chatter` in your IDE (we prefer IntelliJ)
