# Minotaur Problems
## Introduction
This project covers a key concept in parallel programming, mutual exclusion.
Each part of touches a different area of mutual exclusion. In general, part 1
is the prisoner problem from chapter 1 of the book. Essentially, a group of
people (threads in this case) must not communicate with each other, and must
find a way to find out when each person has accessed a boolean value, they can
toggle the boolean value or leave it alone. The second part gives us the option
on how we want to implement mutual exclusivity.
## Part 1 - The Prisoner Problem
The prisoner problem is rather easy to solve especially in this instance where
the threads know the start state of the boolean value. The solution is simple,
if the boolean is true, and you have not set it to true yet, set it to false.
If the boolean is false, and that prisoner is not the "master" prisoner, then
leave the boolean as is. If the prisoner is the "master" prisoner, then reset
the boolean to true and increment the count. Once the count is equal to the number
of prisoners, notify the warden that everyone has been to the room.
### The Implementation
Java has synchronized blocks and the ability to wait on a certain object until notified.
Notification in java is non-deterministic and unfair, meaning that regardless of
when a thread starts to wait, the scheduler chooses a random thread to wake when
`object.notify()` is called, this is how I implemented the random selection of prisoners.
The minotaur (or warden) then waits on another object until the current thread in the room
follows the rules as written above. Then, the thread in the room will notify the warden's object
to wake him up and allow him to "choose" the next prisoner. If the "master" prisoner, finds
that all prisoners have visited the room, then he wakes up the minotaur and tells him
that they were successful.
### Problems
This part of the project was rather straightforward, most of the problems encountered were
language specific and came from not understanding how `object.wait()`,`object.notify()`,
and `synchronized` worked. After reading through the javadocs and looking at some Sender-Receiver
examples, everything was smooth sailing.
## Part 2 - The Vase Problem
The vase problem is all about how to have mutual exclusivity on looking at the vase.
The options we got are analogous to spin-lock, test-and-set, and the Anderson Queue Lock.
The spin-lock is the option A, basically everyone goes to the door and tries to go in,
as soon as someone enters, everyone keeps checking until they leave. This introduces a sequential bottleneck,
there is also a phenomena where potentially (due to the scheduler) a thread may never get access
to the resource. Option B is a test and set lock, the first person in says that it is busy, the other
threads can now spin waiting for the resource to free up and when a thread leaves they change the boolean value of the lock
to true in order to allow another thread to set the boolean and enter. This has a similar problem to option A, 
there is competition for the resource and there's no guarantee that every thread will get access.
Option C is the best lock as it is the Anderson Queue Lock, it is a test and set lock but in a queue implementation.
Instead of every thread trying to test and set a single boolean value, they just wait for their turn in line. The thread that is
leaving will notify the next thread to enter. Every thread is guaranteed to enter. The issue is the implementation, one bit per thread becomes
one cache line per thread as there is potential that some bit might be stored across threads. I did not run into this issue 
because a boolean in Java is a whole byte long.
### The Implementation
Implementing the lock was rather simple just following the slides as they are written in Java.
Essentially, the lock creates a boolean array with the number of slots for the number of threads that 
will be queuing. The lock is implemented with the first index set to true, thus the first thread to try
to acquire the lock instantly obtains it. Unlock just sets the next value in the array to true, allowing the next
thread to access the resource. 
### Problems
This part technically has no stop point, so I just gave users the ability to choose the time they wanted to allow the program to run for.
The problem was that if a thread is interrupted while waiting on the lock, it is stuck spinning, waiting for it's time to shine.
I fixed this problem by implementing `lockInterruptibly()` which throws an interrupted exception if the thread trying to
acquire the lock is interrupted before it can acquire the lock, essentially killing the thread in the implementation.

## Setup and Running
First and foremost, make sure that you have the JDK installed as well as a JRE.

If you are on Windows you can clone this repo with:

```git clone https://github.com/NChitty/MinotaurThreads.git```

Once this is done, just run the `compile.bat` file in the `build` directory, then move on to running the program.

If you are on Linux or Mac, navigate to the `src` directory and run the following commands:

```
javac -d ./build Main.java
cd ./build
jar -cvfm Minotaur.jar manifest.txt *.class
```

This program has some arguments involved, in general the syntax for running the program is 
`java -jar Minotaur.jar [part] [options]`.

Look at the table below for the options:
| Part    | 1 | 2 |
| :--- | --- | --- |
| Options | The user can choose the number of threads by running `java -jar Minotaur.jar 1 [number of threads]`. This is purely optional, by default however, the number of threads is 8. | For part two there are two options, the number of threads and the runtime. To denote the number of threads, use `-t [number of threads]`, 
again, by default, the number of threads is 8. To choose the run time, use `-r [runtime in seconds]`, this only supports integer
value of seconds. |
