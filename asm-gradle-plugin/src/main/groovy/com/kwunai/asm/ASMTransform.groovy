package com.kwunai.asm

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.internal.pipeline.TransformManager
import groovy.io.FileType
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils

import javax.xml.crypto.dsig.TransformException

class ASMTransform extends Transform {

    @Override
    String getName() {
        return "AmsTransformTest"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation)
            throws TransformException, InterruptedException, IOException {


        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider()
        outputProvider.deleteAll()


        transformInvocation.getInputs().each { TransformInput input ->

            //遍历目录
            input.directoryInputs.each { DirectoryInput directoryInput ->
                //获取 output 目录
                def dest = outputProvider.getContentLocation(directoryInput.name,
                        directoryInput.contentTypes, directoryInput.scopes,
                        Format.DIRECTORY)
                File dir = directoryInput.file
                HashMap<String, File> modifyMap = new HashMap<>()
                if (dir) {
                    // 遍历获取以class结尾的文件
                    dir.traverse(type: FileType.FILES, nameFilter: ~/.*\.class/) {
                        File classFile ->
                            // 扫描所有包中的类，获取到正常的类
                            // 找到你编写的类，例如MainActivity
                            if (AMSClassModifier.isShouldModify(classFile.name)) {
                                // 1.扫描方法，有没有注解，AMS注入

                                // 得到新注入后的class文件
                                File modified = AMSClassModifier.modifyClassFile(dir, classFile,
                                        transformInvocation.getContext().getTemporaryDir())

                                if (modified != null) {
                                    String key = classFile.absolutePath
                                            .replace(dir.absolutePath, "")
                                    modifyMap.put(key, modified)
                                }
                            }
                    }
                }

                // 将 input 的目录复制到 output 指定目录
                FileUtils.copyDirectory(directoryInput.file, dest)

                // 替换注入的新文件
                modifyMap.entrySet().each { Map.Entry<String, File> en ->
                    File target = new File(dest.absolutePath + en.getKey())
                    if (target.exists()) {
                        target.delete()
                    }
                    FileUtils.copyFile(en.getValue(), target)
                    en.getValue().delete()
                }
            }

            //遍历 jar
            input.jarInputs.each { JarInput jarInput ->
                // 重命名输出文件（同目录copyFile会冲突）
                def jarName = jarInput.name
                def md5Name = DigestUtils.md5(jarInput.file.getAbsolutePath())
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                }

                File copyJarFile = jarInput.file

                //生成输出路径
                def dest = outputProvider.getContentLocation(jarName + md5Name,
                        jarInput.contentTypes, jarInput.scopes, Format.JAR)
                // 将 input 的目录复制到 output 指定目录
                FileUtils.copyFile(copyJarFile, dest)
            }
        }
    }
}
