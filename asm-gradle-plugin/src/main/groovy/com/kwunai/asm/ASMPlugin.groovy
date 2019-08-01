package com.kwunai.asm

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class ASMPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        AppExtension extension = project.extensions.findByType(AppExtension.class)
        extension.registerTransform(new ASMTransform())
    }
}