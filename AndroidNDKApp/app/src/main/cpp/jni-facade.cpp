//
// Created by TW0444 on 2023/12/18.
//
#include <jni.h>
#include "fibonacci.h"
#include <stdio.h>
#include <string>


extern "C"
JNIEXPORT jint JNICALL
Java_com_example_androidndkapp_MainActivity_computeFibonacciNative(
        JNIEnv *env,
        jobject /* this */,
        jint n
        ) {
    // compute the result and convert it to jint before passing back to Java
    jint result = static_cast<jint>(computeFibonacci(n));

    // construct an instance of FibonacciResult object defined in Java code
//    jclass resultClass = env->FindClass("com/example/androidndkapp/FibonacciResult");
//    jclass resultClass = env->FindClass("com/example/androidndkapp/FibonacciResult");
//    jmethodID constructor = env->GetMethodID(resultClass, "<init>", "(II)V");
//    jobject resultObj = env->NewObject(resultClass, constructor, n, result);

//    return resultObj;
//    char buf[64];
//    std::string res="";
//    sprintf(res, "%s", result);
//    return env->NewStringUTF(res.c_str());
    return result;
}