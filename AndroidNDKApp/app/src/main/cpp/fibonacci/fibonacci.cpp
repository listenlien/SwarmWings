//
// Created by TW0444 on 2023/12/18.
//
int computeFibonacci(int n) {
    if (n<=1) {
        return n;
    }
    return computeFibonacci(n - 1) + computeFibonacci(n - 2);
}