# P2P-Maze-Game
A demo game for peer-to-peer distributed system

Instruction to start demo:

##### Go to java source folder
```
cd P2P-Maze-Game/src/
```

##### compile class into build dir
```
javac -d ../build/ com/p2p/maze/*.java
```
Note: to run StressTest, please compile the class into `/build` dir as well

##### Open a new terminal to start rmiregistry (port 1099)
```
rmiregistry 1099
```

##### Open a new terminal to Start Tracker
Input format
> java com.p2p.maze.Tracker [Port Number] [N] [K]
```
java com.p2p.maze.Tracker 1099 15 10
```

##### Open a new terminal to Start Game
Input format
> java com.p2p.maze.Game [IP Address] [Port Number] [Player ID (two char)]
```
java com.p2p.maze.Game localhost 1099 p1
```


##### Run Stress Test
If to run StressTest, run the following command in `/build` dir
```
java StressTest 127.0.0.1 1099 "java com.p2p.maze.Game"
```
