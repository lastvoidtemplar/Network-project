package client;


public class ResultChecker {
    private final long[] requestArr;
    private final long[] responseArr;

    ResultChecker(long[] requestArr, long[] responseArr) {
        this.requestArr = requestArr;
        this.responseArr = responseArr;
    }

    private boolean IsSorted() {
        for (int i = 1; i < responseArr.length; i++) {
            if (responseArr[i - 1] > responseArr[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean binarySearch(long curr) {
        int l = 0;
        int r = responseArr.length - 1;
        while (l <= r) {
            int m = (l + r) / 2;
            long el = responseArr[m];
            if (el == curr) {
                return true;
            } else if (curr < el) {
                r = m - 1;
            } else {
                l = m + 1;
            }
        }
        return false;
    }

    private boolean doesContainSameElements() {
        for (int i = 0; i < requestArr.length; i++) {
            if (!binarySearch(requestArr[i])){
                return false;
            }
        }
        return true;
    }

    public boolean IsCorrect() {
        return requestArr.length == responseArr.length && IsSorted() && doesContainSameElements();
    }
}
