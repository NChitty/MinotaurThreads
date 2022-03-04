import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class Main {



    static Object waitForGuest = new Object();
    static boolean notified = false;

    public static void main(String[] args) {
        if(args.length < 1) {
            System.out.println("Please run this program using \"java -jar Minotaur.jar [1|2] [option]\"");
            System.out.println("The only option for part 1 is the number of threads. Not choosing the number of threads will default to 8 threads.");
            System.out.println("The options for part 2 are formatted as follows: -t [# of threads] or -r [runtime seconds]");
            return;
        }
        String part = args[0];
        if(Integer.parseInt(part) == 1) {
            int numberOfThreads = 8;
            if(args.length >= 2) {
                try {
                    numberOfThreads = Integer.parseInt(args[1]);
                } catch (NumberFormatException nfe) {
                    System.out.println("The number of threads must be a number.");
                    return;
                }
            }
            labyrinthProblem(numberOfThreads);
        } else if(Integer.parseInt(part) == 2) {
            int seconds = 10;
            int numberOfThreads = 8;
            if(args.length >= 2) {
                for(int i = 1; i < args.length; i++) {
                    if(args[i].equals("-t")) {
                        i++;
                        try {
                            numberOfThreads = Integer.parseInt(args[i]);
                        } catch (NumberFormatException nfe) {
                            System.out.println("The number of threads must be a number.");
                            return;
                        }
                    } else if(args[i].equals("-r")) {
                        i++;
                        try {
                            seconds = Integer.parseInt(args[i]);
                        } catch (NumberFormatException nfe) {
                            System.out.println("The runtime must be a number.");
                            return;
                        }
                    }
                }
            }
            vaseProblem(numberOfThreads, seconds);
        } else {
            System.out.println("I do not know what part you are trying to run.");
        }
    }

    public static void labyrinthProblem(int numberOfGuests) {
        Labyrinth labyrinth = new Labyrinth(numberOfGuests);

        Thread[] guests = new Thread[numberOfGuests];
        for(int i = 0; i < numberOfGuests; i++) {
            if(i == 0) {
                guests[i] = new MasterGuestThread(labyrinth, numberOfGuests);
            } else {
                guests[i] = new GuestThread(labyrinth, i);
            }
            guests[i].start();
        }
        int guestsSelected = 0;
        while(!notified) {
            guestsSelected++;
            synchronized (labyrinth) {
                labyrinth.notify();
            }
            try {
                synchronized (waitForGuest) {
                    waitForGuest.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Successfully solved puzzle after " + guestsSelected + " had been selected.");
        labyrinth.printGuestsVisited();
        for(Thread guest : guests) {
            guest.interrupt();
        }
    }

    public static void vaseProblem(int numberOfGuests, int seconds) {
        Thread[] threads = new Thread[numberOfGuests];
        Lock lock = new AndersonQueueLock(numberOfGuests);
        for(int i = 0; i < numberOfGuests; i++) {
            threads[i] = new VaseGuestThread(i, lock);
            threads[i].start();
        }
        long startTime = System.currentTimeMillis();
        while((System.currentTimeMillis() - startTime) < seconds*1000) {}
        notified = true;
        for(Thread thread : threads) {
            thread.interrupt();
        }
    }

    static class GuestThread extends Thread {
        Labyrinth labyrinth;
        boolean hasEatenCupcake = false;
        final int number;

        public GuestThread(Labyrinth labyrinth, int number) {
            this.labyrinth = labyrinth;
            this.number = number;
        }

        public void run() {
            while(!this.isInterrupted()) {
                try {
                    synchronized (labyrinth) {
                        labyrinth.wait();
                    }
                    labyrinth.guestsVisited[number] = true;
                    if (labyrinth.isCupcake() && !hasEatenCupcake) {
                        labyrinth.eatCupcake();
                        hasEatenCupcake = true;
                    }
                    // else do nothing and exit room
                    synchronized (waitForGuest) {
                        waitForGuest.notify();
                    }
                } catch (InterruptedException e) {
                    return;
                }
            }
        }

    }

    static class MasterGuestThread extends Thread {
        Labyrinth labyrinth;
        final int guests;
        int count = 0;

        public MasterGuestThread(Labyrinth labyrinth, int numberOfGuests) {
            guests = numberOfGuests;
            this.labyrinth = labyrinth;
        }

        public void run() {
            while (!this.isInterrupted()) {
                try {
                    synchronized (labyrinth) {
                        labyrinth.wait();
                    }
                    labyrinth.guestsVisited[0] = true;
                    if (!labyrinth.isCupcake()) {
                        this.count++;
                        System.out.printf("Tracker guest counted %d guests have gone through the labyrinth.\n", this.count + 1);
                        if (count + 1 == guests) {
                            notified = true;
                            synchronized (waitForGuest) {
                                waitForGuest.notify();
                            }
                            return;
                        }
                        labyrinth.askForNewCupcake();
                    }
                    synchronized (waitForGuest) {
                        waitForGuest.notify();
                    }
                } catch (InterruptedException ie) {

                }
            }
        }
    }

    static class Labyrinth {
        private boolean cupcake = true;
        private boolean[] guestsVisited;

        public Labyrinth(int numberOfGuests) {
            guestsVisited = new boolean[numberOfGuests];
        }

        public void printGuestsVisited() {
            for(int i = 0; i < guestsVisited.length; i++) {
                System.out.printf("%d\t", i);
            }
            System.out.println();
            for(int i = 0; i < guestsVisited.length; i++) {
                System.out.printf("%s\t", guestsVisited[i] ? "T" : "F");
            }
            System.out.println("\n");
        }

        public void eatCupcake() {
            this.cupcake = false;
        }

        public void askForNewCupcake() {
            this.cupcake = true;
        }

        public boolean isCupcake() {
            return this.cupcake;
        }

    }

    static class VaseGuestThread extends Thread {
        enum State {
            WAITING("WAITING"), LOOKING_AT_VASE("LOOKING AT VASE"), ROAMING_CASTLE("ROAMING CASTLE");

            final String state;

            State(String s) {
                state = s;
            }

            @Override
            public String toString() {
                return state;
            }
        }

        private State state = State.ROAMING_CASTLE;
        private Lock lock;
        private long startWait;

        public VaseGuestThread(int n, Lock lock) {
            super("Guest " + n);
            this.lock = lock;
        }

        @Override
        public void run() {
            while(!this.isInterrupted() && !notified) {
                switch (state) {
                    case LOOKING_AT_VASE:
                        state = State.ROAMING_CASTLE;
                        lock.unlock();
                        break;
                    case ROAMING_CASTLE:
                        double choice = Math.random();
                        if(choice > .5) {
                            state = State.WAITING;
                            startWait = System.currentTimeMillis();
                            try {
                                lock.lockInterruptibly();
                            } catch (InterruptedException ie) {
                                return;
                            } finally {
                                System.out.printf("%s waited %dms to look at the vase.\n", this.getName(), System.currentTimeMillis() - startWait);
                                state = State.LOOKING_AT_VASE;
                            }
                        }
                        break;
                    case WAITING:
                        break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {}
            }
        }

        @Override
        public String toString() {
            return this.getName() + ": " + state;
        }

    }

    static class AndersonQueueLock implements Lock {
        boolean[] flags;
        AtomicInteger next = new AtomicInteger(0);
        ThreadLocal<Integer> mySlot;
        final int n;

        public AndersonQueueLock(int threads) {
            flags = new boolean[threads];
            flags[0] = true;
            n = threads;
            mySlot = new ThreadLocal<>();
        }

        @Override
        public void lock() {
            mySlot.set(next.getAndIncrement());
            while(!flags[mySlot.get() % n]) {}
            flags[mySlot.get() % n] = false;
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            mySlot.set(next.getAndIncrement());
            while(!flags[mySlot.get() % n]) {
                if(Thread.currentThread().isInterrupted())
                    throw new InterruptedException();
            }
            flags[mySlot.get() % n] = false;
        }

        @Override
        public boolean tryLock() {
            return false;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return false;
        }

        @Override
        public void unlock() {
            flags[(mySlot.get() + 1) % n] = true;
        }

        @Override
        public Condition newCondition() {
            return null;
        }
    }
}
