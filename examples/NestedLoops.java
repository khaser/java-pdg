public class NestedLoops {
    public static int nestedLoops(int[] arr) {
        int sum = 0;
        for (int i = 0; i < arr.length; i++) {
            int value = arr[i];
            sum = sum + value;
        }
        return sum;
    }
}

