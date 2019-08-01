package com.kwunai.ams

import com.kwunai.ams.annotations.VitalTime

import org.objectweb.asm.*
import org.objectweb.asm.commons.AdviceAdapter


/**
 * 方法扫描器
 */
class AMSMethodAdapterVisitor extends AdviceAdapter {

    private boolean inject
    private int start
    private int end
    private int index

    protected AMSMethodAdapterVisitor(MethodVisitor mv, int access,
                                      String name, String desc) {
        super(Opcodes.ASM6, mv, access, name, desc)
    }

    /**
     * 分析方法上的注解，判断是不是VitalTime，是的话才插桩
     */
    @Override
    AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (Type.getDescriptor(VitalTime.class).equals(desc)) {
            inject = true
        }
        return super.visitAnnotation(desc, visible);
    }

    /**
     * 方法进入
     */
    @Override
    protected void onMethodEnter() {
        if (inject) {
            // long var1 = System.currentTimeMillis
            //  invokestatic  #2  // Method java/lang/System.currentTimeMillis:()J
            //         3: lstore_1
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System",
                    "currentTimeMillis", "()J", false)
            // lstore压栈获取返回值
            index = newLocal(Type.LONG_TYPE)
            start = index
            mv.visitVarInsn(LSTORE, start)
        }
    }

    /**
     * 方法退出
     * @param opcode
     */
    @Override
    protected void onMethodExit(int opcode) {
        //  long var2 = System.currentTimeMillis();
        // invokestatic  #2 // Method java/lang/System.currentTimeMillis:()J
        //         7: lstore_3
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/System",
                "currentTimeMillis", "()J", false)
        index = newLocal(Type.LONG_TYPE)
        end = index
        mv.visitVarInsn(LSTORE, end)

        // 执行减法

        // System.out
        mv.visitFieldInsn(GETSTATIC, "java/lang/System",
                "out", "Ljava/io/PrintStream;")

        // 这里我们使用了+ ，因为String是不可修改的，每次修改都是一个新对象，
        // 而+就是通过StringBuilder进行的字符串拼接
        //  new   // class java/lang/StringBuilder
        //  dup
        mv.visitTypeInsn(NEW, "java/lang/StringBuilder")
        mv.visitInsn(DUP)

        // 执行StringBuilder的init方法
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder",
                "<init>", "()V", false)

        // 把常量压入栈顶
        mv.visitLdcInsn("execute.")

        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder",
                "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false)

        // 进行减法，将end，start压入栈中
        mv.visitVarInsn(LLOAD, end)
        mv.visitVarInsn(LLOAD, start)

        // 减法指令
        mv.visitInsn(LSUB)

        // 结果append
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder",
                "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false)

        mv.visitLdcInsn("ms.")

        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder",
                "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false)

        // 调用toString
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder",
                "toString", "()Ljava/lang/String;", false)

        // 输出结果
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream",
                "println", "(Ljava/lang/String;)V", false)
    }
}