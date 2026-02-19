public class ForLoopArray {
    public static int forLoopArray(int[] arr) {
        int sum = 0;
        for (int i = 0; i < arr.length; i++) {
            int value = arr[i];
            sum = sum + value;
        }
        return sum;
    }
}

