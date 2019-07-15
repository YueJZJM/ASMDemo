package com.yuejz.myplugin

import org.gradle.internal.impldep.bsh.org.objectweb.asm.ClassVisitor
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;
/**
 * 对继承自AppCompatActivity的Activity进行插桩
 */

class TraceVisitor extends ClassVisitor {

    /**
     * 类名
     */
    private String className;

    /**
     * 父类名
     */
    private String superName;

    /**
     * 该类实现的接口
     */
    private String[] interfaces;

    private ClassVisitor mClassVisitor

    public TraceVisitor(String className, ClassVisitor classVisitor) {
        super(Opcodes.ASM5, classVisitor);
        this.mClassVisitor = classVisitor
    }

    /**
     * ASM进入到类的方法时进行回调
     *
     * @param access
     * @param name 方法名
     * @param desc
     * @param signature
     * @param exceptions
     * @return
     */
    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature,
                                     String[] exceptions) {
        MethodVisitor methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions);
        methodVisitor = new AdviceAdapter(Opcodes.ASM5, methodVisitor, access, name, desc) {

            private boolean isInject() {
                //如果父类名是AppCompatActivity则拦截这个方法,实际应用中可以换成自己的父类例如BaseActivity
                if (superName.contains("AppCompatActivity")) {
                    return true;
                }
                return false;
            }

//            @Override
//            public void visitCode() {
//                super.visitCode();
//
//            }
//
//            @Override
//            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
//                return super.visitAnnotation(desc, visible);
//            }
//
//            @Override
//            public void visitFieldInsn(int opcode, String owner, String name, String desc) {
//                super.visitFieldInsn(opcode, owner, name, desc);
//            }


            /**
             * 方法开始之前回调
             */
            @Override
            protected void onMethodEnter() {
                if (isInject()) {
                    if ("onCreate".equals(name)) {
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitMethodInsn(INVOKESTATIC,
                                "com/yuejianzhong/utils/TraceUtil",
                                "onActivityCreate", "(Landroid/app/Activity;)V",
                                false);
                        println "Hooked method: ${name}${desc}\n"
                    }
//                    else if ("onDestroy".equals(name)) {
//                        mv.visitVarInsn(ALOAD, 0);
//                        mv.visitMethodInsn(INVOKESTATIC, "com/yuejianzhong/utils/TraceUtil"
//                                , "onActivityDestroy", "(Landroid/app/Activity;)V", false);
//                        println "Hooked method: ${name}${desc}\n"
//                    }
                }
            }

            /**
             * 方法结束时回调
             * @param i
             */
            @Override
            protected void onMethodExit(int i) {
                super.onMethodExit(i);
            }
        };
        return methodVisitor;

    }

    /**
     * 当ASM进入类时回调
     *
     * @param version
     * @param access
     * @param name 类名
     * @param signature
     * @param superName 父类名
     * @param interfaces 实现的接口名
     */
    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
        this.superName = superName;
        this.interfaces = interfaces;
        println "开始扫描类：${name}"
        println "类详情：version=${version};\taccess=${access};\tname=${name};\tsignature=${signature};\tsuperName=${superName};\tinterfaces=${interfaces.toArrayString()}\n"

    }

    @Override
    void visitEnd() {
        super.visitEnd()
        MethodVisitor mv
        if (isSupperActivity(superName)) {

            def methodName = "onDestroy"
            def methodDesc = "()V"
            def methodParmsStart = 0
            def methodCount = 1
            int methodCellOpcodes = Opcodes.ALOAD
            def HookConfig = "com/yuejianzhong/utils/TraceUtil"

            mv = mClassVisitor.visitMethod(Opcodes.ACC_PUBLIC,methodName, methodDesc, null, null)

            mv.visitCode()
            // call super
            visitMethodWithLoadedParams(mv, Opcodes.INVOKESPECIAL, superName, methodName, methodDesc,
                    methodParmsStart, methodCount, methodCellOpcodes)
//            println "methodCell.name :" + methodCell.name + ",methodCell.desc :" + methodCell.desc + ",methodCell.paramsStart :" + methodCell.paramsStart +
//                    ",methodCell.paramsCount :" + methodCell.paramsCount + ",methodCell.opcodes :" + methodCell.opcodes
//            // call injected method
            visitMethodWithLoadedParams(mv, Opcodes.INVOKESTATIC, HookConfig, "onActivityDestroy", "(Landroid/app/Activity;)V",
                    0,1, methodCellOpcodes)
            mv.visitInsn(Opcodes.RETURN)
            mv.visitMaxs(1, 1)
            mv.visitEnd()
            mv.visitAnnotation("Lcom/yuejianzhong/utils/Instrument;", false)
//            com.yuejianzhong.utils.Instrument
            println "结束扫描类：${className}\n"
        }
    }

    boolean isSupperActivity(String name) {
        String activityName = "android/support/v7/app/AppCompatActivity"
        if (name.contains(activityName)) {
            return true
        }
        return false
    }

    private static void visitMethodWithLoadedParams(MethodVisitor methodVisitor, int opcode, String owner, String methodName, String methodDesc, int start, int count,int paramOpcodes) {
//        for (int i = start; i < start + count; i++) {
            methodVisitor.visitVarInsn(paramOpcodes, 0)
//        }
        methodVisitor.visitMethodInsn(opcode, owner, methodName, methodDesc, false)
    }

}