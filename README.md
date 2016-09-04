# P2P-Maze-Game
A demo game for peer-to-peer distributed system

Instruction to start demo:

1. Go to java source folder
cd ~/P2P-Maze-Game/src/
cd /c/MyProjects/P2P-Maze-Game/src
2. compile the classes:
javac com/p2p/maze/*.java
3.  Open a new terminal to start rmiregistry
4.  Open a new terminal to Start Tracker
java -Djava.rmi.server.hostname=[A.B.C.D] com.p2p.maze.Tracker [portNumber] [n] [k]
e.g. java -Djava.rmi.server.hostname=A.B.C.D com.p2p.maze.Tracker 3234 32 5
Note: A.B.C.D is IP address, currently portNumber is not in use.
5.  Open a new terminal to Start Game
java com.p2p.maze.Game [A.B.C.D] [portNumber] [playerId]
e.g. java com.p2p.maze.Game [A.B.C.D] 3432 t1
Note: A.B.C.D is IP address, currently ip and portNumber is not in use.
6. Repeat step 5 to create more players.
