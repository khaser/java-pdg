public class LoopWithContinue {
    public static int loopWithContinue(int n) {
        int result = 1;
        int i = 1;
        while (i <= n) {
            result = result * i;
            if (i > 100) {
                continue;
            } else {
                i = i + 1;
                int j = 5;
                while (j < 1000) {
                    if (i * j > 404) {
                        continue;
                    }
                    j = j + 1;
                }
                i -= j;
            }
        }
        return result;
    }
}

