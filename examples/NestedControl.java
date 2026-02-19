public class NestedControl {
    public static int nestedControl(int a, int b, int c) {
        int max = a;
        if (b > max) {
            max = b;
        }
        if (c > max) {
            max = c;
        }

        if (max > 10) {
            int i = 0;
            while (i < max) {
                i = i + 1;
            }
            max = i;
        }

        return max;
    }
}

