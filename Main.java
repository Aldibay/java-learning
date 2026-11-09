class RequestTask implements Runnable {
    private final int requestNumber;
    private final int delayMillis;

    public RequestTask(int requestNumber, int delayMillis) {
        this.requestNumber = requestNumber;
        this.delayMillis = delayMillis;
    }

    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();
        String[] stages = {"Тіркеу", "Тексеру", "Аяқтау"};

        for (String stage : stages) {
            System.out.println("[" + threadName + "] №" + requestNumber
                    + " өтінім: " + stage + " кезеңі");

            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[" + threadName + "] №" + requestNumber
                        + " өтінімді өңдеу үзілді.");
                return;
            }
        }

        System.out.println("[" + threadName + "] №" + requestNumber
                + " өтінім толық өңделді.");
    }
}

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Thread[] threads = {
                new Thread(new RequestTask(101, 300), "Өңдеуші-1"),
                new Thread(new RequestTask(102, 450), "Өңдеуші-2"),
                new Thread(new RequestTask(103, 600), "Өңдеуші-3")
        };

        System.out.println("24-тақырып: Өтінімдерді өңдеу");
        System.out.println("Өтінімдер бір уақытта өңделе бастады.\n");

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println("\nБарлық өтінімдердің өңделуі аяқталды!");
    }
}
