package qosprober.main;

class RunClient implements Runnable {
        private Client client;
        private boolean shouldRun = true;

        public RunClient(Client client) {
            this.client = client;
        }

        public void run() {
            while (shouldRun) {
                try {
                    client.interactWithServer();
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                    }
                } catch (Exception e) {
                    shouldRun = false;
                }
            }
        }
    }