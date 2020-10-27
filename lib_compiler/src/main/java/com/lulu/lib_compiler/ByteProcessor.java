package com.lulu.lib_compiler;

import com.lulu.lib_annotations.BindView;
import com.lulu.lib_annotations.ByteService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;


public class ByteProcessor extends AbstractProcessor {
    private Filer filer;//用于生成新的java文件的对象
    private Messager messager;//用于输出log的对象
    private Map<String, String> mapper = new HashMap<>();
    private long time;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        filer = processingEnvironment.getFiler();
        messager = processingEnvironment.getMessager();
    }

    /**
     * 获取指定注解的所有元素
     * 拿到元素的类名
     * 拿到元素标注的接口的类名
     * 存入map，输出log
     * 开始生成代码
     * @param annotations
     * @param roundEnvironment
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        handleByteService(roundEnvironment);
        handleBindView(roundEnvironment);
        return true;
    }

    private void handleByteService(RoundEnvironment roundEnvironment) {
        //获取该注解的元素
        Set<? extends Element> sets = roundEnvironment.getElementsAnnotatedWith(ByteService.class);

        if (sets != null && sets.size() > 0) {
            Logger.println("将要处理 ByteService 注解：" + sets);
            time = System.currentTimeMillis();
            for (Element element : sets) {
                //每一个元素由于只能是类，所以都是TypeElement类型
                if (element instanceof TypeElement) {
                    //获取定义你该注解的元素(这里是类)的全路径名称
                    String implName = TypeName.get(element.asType()).toString();
                    //对应的接口全路径类名
                    String interName;
                    try {
                        //通过注解的clazz对象直接获取
                        interName = element.getAnnotation(ByteService.class).clazz().getCanonicalName();
                    } catch (MirroredTypeException mte) {
                        //由于调用clazz对象时，可能因为Class对象还没有被加载，所以抛异常
                        //异常中有相关class对象的信息，直接拿到类名即可
                        interName = TypeName.get(mte.getTypeMirror()).toString();
                    }
                    //如果没有定义你clazz(默认为Object)，则取该类默认实现的接口
                    if (Object.class.getCanonicalName().equals(interName)) {
                        List<? extends TypeMirror> typeMirrors = ((TypeElement) element).getInterfaces();
                        interName = TypeName.get(typeMirrors.get(0)).toString();
                    }
                    //放入map中后续生成代码
                    mapper.put(interName, implName);
                    //messager输出log
                    //messager.printMessage(Diagnostic.Kind.NOTE, "保存：Interface: " + interName + " Impl: " + implName);
                    Logger.println("保存：Interface: " + interName + " Impl: " + implName);
                }
            }
            //生成代码
            generate();
            Logger.println("总耗时：" + (System.currentTimeMillis()-time)/1000f+ " s");
        }
    }

    private @Nullable
    TypeElement getSuperClass(TypeElement typeElement) {
        TypeMirror type = typeElement.getSuperclass();
        if (type.getKind() == TypeKind.NONE) {
            return null;
        }
        return (TypeElement) ((DeclaredType) type).asElement();
    }

    /**
     * MethodSpec生成方法和构造器
     * FieldSpec生成字段
     * TypeVariableName生成泛型定义
     * ParameterizedTypeName生成类型，可以包含泛型
     * TypeSpec生成类
     * JavaFile生成文件
     * Filer提供写入文件功能
     */
    private void generate() {
        //private constructor
        MethodSpec cons = MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build();
        //static map
        ParameterizedTypeName mapType = ParameterizedTypeName.get(ClassName.get(Map.class), ClassName.get(Class.class), ClassName.get(Object.class));
        FieldSpec map = FieldSpec.builder(mapType, "services", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL).initializer("new java.util.HashMap<>()").build();
        //static init
        FieldSpec init = FieldSpec.builder(Boolean.class, "isInit", Modifier.PRIVATE, Modifier.STATIC).initializer("false").build();
        //static getService
        MethodSpec.Builder getServiceBuilder = MethodSpec.methodBuilder("getService").addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        TypeVariableName t = TypeVariableName.get("T");
        TypeVariableName b = TypeVariableName.get("B").withBounds(t);
        getServiceBuilder.addTypeVariable(t).addTypeVariable(b);
        getServiceBuilder.addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), t), "clazz");
        getServiceBuilder.returns(b);
        //statement
        getServiceBuilder.beginControlFlow("if(!isInit)");
        generateInitStatement(getServiceBuilder).addStatement("isInit=true").endControlFlow();
        getServiceBuilder.addStatement("return (B) services.get(clazz)");
        //class
        TypeSpec typeSpec = TypeSpec.classBuilder("ServiceManager")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addField(init)
                .addField(map)
                .addMethod(cons)
                .addMethod(getServiceBuilder.build())
                .build();
        //file
        JavaFile javaFile = JavaFile.builder("com.util.service", typeSpec).build();
        try {
            javaFile.writeTo(filer);
        } catch (IOException e) {
        }
    }

    private MethodSpec.Builder generateInitStatement(MethodSpec.Builder getServiceBuilder) {
        for (Map.Entry<String, String> entry : mapper.entrySet()) {
            getServiceBuilder.addStatement(String.format("services.put(%s.class,new %s())", entry.getKey(), entry.getValue()));
        }
        return getServiceBuilder;
    }

    /**
     * getSupportedAnnotationTypes()方法用于返回该Processor想要接收处理的注解，要返回全路径类名，通常使用getCanonicalName()方法。
     * 该方法也可以通过在Processor类上定义SupportedAnnotationTypes注解的方式指定。
     * @return
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> res = new HashSet<>();
        res.add(ByteService.class.getCanonicalName());
        return res;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    public static void error(Element element, String msg, Object... formatStr) {
        System.out.println(Logger.TAG + String.format(msg, (Object) formatStr));
    }


    private void handleBindView(RoundEnvironment roundEnvironment) {
        //获取该注解的元素
        Set<? extends Element> sets = roundEnvironment.getElementsAnnotatedWith(BindView.class);
        if (sets == null || sets.size() <= 0) {
            return;
        }
        for (Element element : sets) {
            TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();
            Logger.println("正在 handleBindView 注解：" + enclosingElement + "\n"
                    + "enclosingElement.getSimpleName() : " + enclosingElement.getSimpleName() + "\n"
                    + "enclosingElement.getQualifiedName() : " + enclosingElement.getQualifiedName() + "\n"
                    + "enclosingElement.getNestingKind() : " + enclosingElement.getNestingKind() + "\n"
            );
        }
    }


    /**
     * 做开放新检查
     * @param annotationClass
     * @param targetThing
     * @param element
     * @return
     */
    private boolean isInaccessibleViaGeneratedCode(Class<? extends Annotation> annotationClass,
                                                   String targetThing, Element element) {
        boolean hasError = false;
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

        // Verify field or method modifiers.
        Set<Modifier> modifiers = element.getModifiers();
        if (modifiers.contains(Modifier.PRIVATE) || modifiers.contains(Modifier.STATIC)) {
            error(element, "@%s %s must not be private or static. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        // Verify containing type.
        if (enclosingElement.getKind() != ElementKind.CLASS) {
            error(enclosingElement, "@%s %s may only be contained in classes. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        // Verify containing class visibility is not private.
        if (enclosingElement.getModifiers().contains(Modifier.PRIVATE)) {
            error(enclosingElement, "@%s %s may not be contained in private classes. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        return hasError;
    }

    /**
     * 不能乱绑定
     * @param annotationClass
     * @param element
     * @return
     */
    private boolean isBindingInWrongPackage(Class<? extends Annotation> annotationClass,
                                            Element element) {
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();
        String qualifiedName = enclosingElement.getQualifiedName().toString();

        if (qualifiedName.startsWith("android.")) {
            error(element, "@%s-annotated class incorrectly in Android framework package. (%s)",
                    annotationClass.getSimpleName(), qualifiedName);
            return true;
        }
        if (qualifiedName.startsWith("java.")) {
            error(element, "@%s-annotated class incorrectly in Java framework package. (%s)",
                    annotationClass.getSimpleName(), qualifiedName);
            return true;
        }

        return false;
    }

}