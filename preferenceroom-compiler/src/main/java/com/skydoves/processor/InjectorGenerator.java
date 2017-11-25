/*
 * Copyright (C) 2017 skydoves
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.skydoves.processor;

import android.support.annotation.NonNull;

import com.google.common.base.VerifyException;
import com.skydoves.preferenceroom.InjectPreference;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.TypeElement;

import static javax.lang.model.element.Modifier.PUBLIC;

public class InjectorGenerator {

    private static final String CLAZZ_PREFIX = "_Injector";
    private static final String INJECT_OBJECT = "injectObject";
    private static final String PREFERENCE_PREFIX = "Preference_";
    private static final String COMPONENT_PREFIX = "PreferenceComponent_";

    private final PreferenceComponentAnnotatedClass annotatedClazz;
    private final TypeElement injectedElement;

    public InjectorGenerator(@NonNull PreferenceComponentAnnotatedClass annotatedClass, @NonNull TypeElement injectedElement) {
        this.annotatedClazz = annotatedClass;
        this.injectedElement = injectedElement;
    }

    public TypeSpec generate() {
        return TypeSpec.classBuilder(getClazzName())
                .addJavadoc("Generated by PreferenceRoom. (https://github.com/skydoves/PreferenceRoom).\n")
                .addModifiers(PUBLIC)
                .addMethod(getConstructorSpec())
                .build();
    }

    public MethodSpec getConstructorSpec() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(ParameterSpec.builder(TypeName.get(injectedElement.asType()), INJECT_OBJECT).addAnnotation(NonNull.class).build());

        injectedElement.getEnclosedElements().stream()
                .filter(field -> field.getKind().isField())
                .forEach(field -> {
                    if(field.getAnnotation(InjectPreference.class) != null) {
                        String annotatedFieldName = TypeName.get(field.asType()).toString();
                        if(annotatedClazz.generatedClazzList.contains(annotatedFieldName)) {
                            builder.addStatement(INJECT_OBJECT + ".$N = " + COMPONENT_PREFIX + "$N.getInstance().$N()",
                                    field.getSimpleName(), annotatedClazz.clazzName, TypeName.get(field.asType()).toString().replace(PREFERENCE_PREFIX, ""));
                        } else if((COMPONENT_PREFIX + annotatedClazz.clazzName).equals(annotatedFieldName)) {
                            builder.addStatement(INJECT_OBJECT + ".$N = " + COMPONENT_PREFIX + "$N.getInstance()",
                                    field.getSimpleName(), annotatedClazz.clazzName);
                        } else {
                            throw new VerifyException(String.format("'%s' type can not be injected", annotatedFieldName));
                        }
                    }
                });

        return builder.build();
    }

    private String getClazzName() {
        return injectedElement.getSimpleName() + CLAZZ_PREFIX;
    }
}