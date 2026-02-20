public class LoopWithBreak {
    public static int loopWithBreak(int n) {
        int result = 1;
        int i = 1;
        while (i <= n) {
            result = result * i;
            if (i > 100) {
                break;
            } else {
                i = i + 1;
                int j = 5;
                while (j < 1000) {
                    if (i * j > 404) {
                        break;
                    }
                    j = j + 1;
                }
                i -= j;
            }
        }
        return result;
    }
}

