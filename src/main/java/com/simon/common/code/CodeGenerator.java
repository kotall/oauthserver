package com.simon.common.code;

import com.google.common.base.CaseFormat;
import freemarker.template.TemplateExceptionHandler;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.generator.api.MyBatisGenerator;
import org.mybatis.generator.config.*;
import org.mybatis.generator.internal.DefaultShellCallback;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 代码生成器，根据数据表名称生成对应的Model、Mapper、Service、Controller简化开发。
 */
public class CodeGenerator {

    private static String BASE_PACKAGE;//项目基础包名称，根据自己的项目修改

    /*生成文件地址配置*/
    private static String MODEL_PACKAGE;//生成的Model类所在包
    private static String MAPPER_PACKAGE;//生成的Mapper所在包
    private static String REPOSITORY_PACKAGE;//生成Repository所在包
    private static String SERVICE_PACKAGE;//生成的Service所在包
    private static String SERVICE_IMPL_PACKAGE;//生成的ServiceImpl所在包
    private static String CONTROLLER_PACKAGE;//生成的Controller所在包

    private static String MAPPER_INTERFACE_REFERENCE;//Mapper插件基础接口的完全限定名(第二步提到的核心继承接口Mapper)

    /*数据库配置*/
    private static String JDBC_URL;//数据库url
    private static String JDBC_USERNAME;
    private static String JDBC_PASSWORD;
    private static String JDBC_DIVER_CLASS_NAME;

    static final String PROJECT_PATH = System.getProperty("user.dir");//项目在硬盘上的基础路径
    static final String TEMPLATE_FILE_PATH = PROJECT_PATH + "/src/main/resources/templates/code";//模板位置

    static String JAVA_PATH;//java文件路径
    static String RESOURCES_PATH;//资源文件路径

    private static String PACKAGE_PATH_REPOSITORY;
    private static String PACKAGE_PATH_SERVICE;//生成的Service存放路径
    private static String PACKAGE_PATH_SERVICE_IMPL;//生成的Service实现存放路径
    private static String PACKAGE_PATH_CONTROLLER;//生成的Controller存放路径

    private static String GEN_MODULES;//要生成的模块

    static String AUTHOR;//@author
    static final String CREATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());//@date

    private static Properties prop;
    static {
        prop = new Properties();
        try {
            prop.load(CodeGenerator.class.getResourceAsStream("/code-gen.properties"));
            JDBC_URL = prop.getProperty("jdbc_url");
            JDBC_USERNAME = prop.getProperty("jdbc_username");
            JDBC_PASSWORD = prop.getProperty("jdbc_password");
            JDBC_DIVER_CLASS_NAME = prop.getProperty("jdbc_driver_class_name");
            AUTHOR = prop.getProperty("author");
            JAVA_PATH = prop.getProperty("java_path");
            RESOURCES_PATH = prop.getProperty("resources_path");
            BASE_PACKAGE = prop.getProperty("base_package");

            MODEL_PACKAGE = BASE_PACKAGE + ".model";
            MAPPER_PACKAGE = BASE_PACKAGE + ".mapper";
            REPOSITORY_PACKAGE = BASE_PACKAGE + ".repository";
            SERVICE_PACKAGE = BASE_PACKAGE + ".service";
            SERVICE_IMPL_PACKAGE = BASE_PACKAGE + ".serviceImpl";
            CONTROLLER_PACKAGE = BASE_PACKAGE + ".controller";
            MAPPER_INTERFACE_REFERENCE = prop.getProperty("mapper_interface_reference");

            PACKAGE_PATH_REPOSITORY = packageConvertPath(REPOSITORY_PACKAGE);
            PACKAGE_PATH_SERVICE = packageConvertPath(SERVICE_PACKAGE);
            PACKAGE_PATH_SERVICE_IMPL = packageConvertPath(SERVICE_IMPL_PACKAGE);
            PACKAGE_PATH_CONTROLLER = packageConvertPath(CONTROLLER_PACKAGE);

            GEN_MODULES = prop.getProperty("gen_modules");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public CodeGenerator(){

    }

    /*main函数入口,放入表名运行即可生成代码*/
    public static void main(String[] args) {
        //genCode("users", "news_info");

        //genCodeByCustomModelName("输入表名","输入自定义Model名称");
        /*genCodeByCustomModelName("authorities", "Authority");
        genCodeByCustomModelName("news_info","NewsInfo");
        genCodeByCustomModelName("users", "OauthUser");
        genCodeByCustomModelName("veri_code", "VeriCode");
        genCodeByCustomModelName("reset_pwd_info", "ResetPwdInfo");
        genCodeByCustomModelName("qr_code", "QrCode");
        genCodeByCustomModelName("log_login", "LogLogin");
        genCodeByCustomModelName("news_tag", "NewsTag");*/
        genCodeByCustomModelName("news_tag", "NewsTag");
    }

    /**
     * 通过数据表名称生成代码，Model 名称通过解析数据表名称获得，下划线转大驼峰的形式。
     * 如输入表名称 "t_user_detail" 将生成 TUserDetail、TUserDetailMapper、TUserDetailService ...
     * @param tableNames 数据表名称...
     */
    public static void genCode(String... tableNames) {
        for (String tableName : tableNames) {
            genCodeByCustomModelName(tableName, null);
        }
    }

    /**
     * 通过数据表名称，和自定义的 Model 名称生成代码
     * 如输入表名称 "t_user_detail" 和自定义的 Model 名称 "User" 将生成 User、UserMapper、UserService ...
     * @param tableName 数据表名称
     * @param modelName 自定义的 Model 名称
     */
    private static void genCodeByCustomModelName(String tableName, String modelName) {
        if(StringUtils.isEmpty(GEN_MODULES)){
            genModelAndMapper(tableName, modelName);
            genRepository(tableName, modelName);
            genService(tableName, modelName);
            //genController(tableName, modelName);
        }else{
            if(GEN_MODULES.toLowerCase().contains("modelandmapper")){
                genModelAndMapper(tableName, modelName);
            }
            if(GEN_MODULES.toLowerCase().contains("repository")){
                genRepository(tableName, modelName);
            }
            if(GEN_MODULES.toLowerCase().contains("service")){
                genService(tableName, modelName);
            }
            if(GEN_MODULES.toLowerCase().contains("controller")){
                genController(tableName, modelName);
            }
        }
    }


    private static void genModelAndMapper(String tableName, String modelName) {
        Context context = new Context(ModelType.FLAT);
        context.setId("Potato");
        context.setTargetRuntime("MyBatis3Simple");
        context.addProperty(PropertyRegistry.CONTEXT_BEGINNING_DELIMITER, "`");
        context.addProperty(PropertyRegistry.CONTEXT_ENDING_DELIMITER, "`");

        JDBCConnectionConfiguration jdbcConnectionConfiguration = new JDBCConnectionConfiguration();
        jdbcConnectionConfiguration.setConnectionURL(JDBC_URL);
        jdbcConnectionConfiguration.setUserId(JDBC_USERNAME);
        jdbcConnectionConfiguration.setPassword(JDBC_PASSWORD);
        jdbcConnectionConfiguration.setDriverClass(JDBC_DIVER_CLASS_NAME);
        context.setJdbcConnectionConfiguration(jdbcConnectionConfiguration);

        PluginConfiguration pluginConfiguration = new PluginConfiguration();
        pluginConfiguration.setConfigurationType("tk.mybatis.mapper.generator.MapperPlugin");
        pluginConfiguration.addProperty("mappers", MAPPER_INTERFACE_REFERENCE);
        context.addPluginConfiguration(pluginConfiguration);

        //该代码不能满足要求
        JavaModelGeneratorConfiguration javaModelGeneratorConfiguration = new JavaModelGeneratorConfiguration();
        javaModelGeneratorConfiguration.setTargetProject(PROJECT_PATH + JAVA_PATH);
        javaModelGeneratorConfiguration.setTargetPackage(MODEL_PACKAGE);
        context.setJavaModelGeneratorConfiguration(javaModelGeneratorConfiguration);

        SqlMapGeneratorConfiguration sqlMapGeneratorConfiguration = new SqlMapGeneratorConfiguration();
        sqlMapGeneratorConfiguration.setTargetProject(PROJECT_PATH + RESOURCES_PATH);
        sqlMapGeneratorConfiguration.setTargetPackage("Mapping");
        context.setSqlMapGeneratorConfiguration(sqlMapGeneratorConfiguration);

        JavaClientGeneratorConfiguration javaClientGeneratorConfiguration = new JavaClientGeneratorConfiguration();
        javaClientGeneratorConfiguration.setTargetProject(PROJECT_PATH + JAVA_PATH);
        javaClientGeneratorConfiguration.setTargetPackage(MAPPER_PACKAGE);
        javaClientGeneratorConfiguration.setConfigurationType("XMLMAPPER");
        context.setJavaClientGeneratorConfiguration(javaClientGeneratorConfiguration);

        TableConfiguration tableConfiguration = new TableConfiguration(context);
        tableConfiguration.setTableName(tableName);
        if (StringUtils.isNotEmpty(modelName)){
            tableConfiguration.setDomainObjectName(modelName);
        }
        tableConfiguration.setGeneratedKey(new GeneratedKey("id", "Mysql", true, null));

        context.addTableConfiguration(tableConfiguration);

        List<String> warnings;
        MyBatisGenerator generator;
        try {
            Configuration config = new Configuration();
            config.addContext(context);
            config.validate();

            boolean overwrite = true;
            DefaultShellCallback callback = new DefaultShellCallback(overwrite);
            warnings = new ArrayList<>();
            generator = new MyBatisGenerator(config, callback, warnings);
            generator.generate(null);
        } catch (Exception e) {
            throw new RuntimeException("生成Model和Mapper失败", e);
        }

        if (generator.getGeneratedJavaFiles().isEmpty() || generator.getGeneratedXmlFiles().isEmpty()) {
            throw new RuntimeException("生成Model和Mapper失败：" + warnings);
        }
        if (StringUtils.isEmpty(modelName)){
            modelName = tableNameConvertUpperCamel(tableName);
        }
        System.out.println(modelName + ".java 生成成功");
        System.out.println(modelName + "Mapper.java 生成成功");
        System.out.println(modelName + "Mapper.xml 生成成功");

        FreeMarkerGeneratorUtil.generatorMvcCode(
                JDBC_DIVER_CLASS_NAME,
                JDBC_URL,
                JDBC_USERNAME,
                JDBC_PASSWORD,
                tableName,
                modelName,
                MODEL_PACKAGE);
    }

    private static void genRepository(String tableName, String modelName) {
        try {
            freemarker.template.Configuration cfg = getConfiguration();

            Map<String, Object> data = new HashMap<>();
            data.put("AUTHOR", AUTHOR);
            data.put("CREATE", CREATE);
            String modelNameUpperCamel = StringUtils.isEmpty(modelName) ? tableNameConvertUpperCamel(tableName) : modelName;
            data.put("modelNameUpperCamel", modelNameUpperCamel);
            data.put("modelNameLowerCamel", modelNameConvertLowerCamel(modelNameUpperCamel));
            data.put("basePackage", BASE_PACKAGE);

            File file = new File(PROJECT_PATH + JAVA_PATH + PACKAGE_PATH_REPOSITORY + modelNameUpperCamel + "Repository.java");
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            cfg.getTemplate("repository.ftl").process(data,
                    new FileWriter(file));
            System.out.println(modelNameUpperCamel + "Repository.java 生成成功");

        } catch (Exception e) {
            throw new RuntimeException("生成repository失败", e);
        }
    }

    private static void genService(String tableName, String modelName) {
        try {
            freemarker.template.Configuration cfg = getConfiguration();

            Map<String, Object> data = new HashMap<>();
            data.put("AUTHOR", AUTHOR);
            data.put("CREATE", CREATE);
            String modelNameUpperCamel = StringUtils.isEmpty(modelName) ? tableNameConvertUpperCamel(tableName) : modelName;
            data.put("modelNameUpperCamel", modelNameUpperCamel);
            data.put("modelNameLowerCamel", modelNameConvertLowerCamel(modelNameUpperCamel));
            data.put("basePackage", BASE_PACKAGE);

            File file = new File(PROJECT_PATH + JAVA_PATH + PACKAGE_PATH_SERVICE + modelNameUpperCamel + "Service.java");
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            cfg.getTemplate("service.ftl").process(data,
                    new FileWriter(file));
            System.out.println(modelNameUpperCamel + "Service.java 生成成功");

            File file1 = new File(PROJECT_PATH + JAVA_PATH + PACKAGE_PATH_SERVICE_IMPL + modelNameUpperCamel + "ServiceImpl.java");
            if (!file1.getParentFile().exists()) {
                file1.getParentFile().mkdirs();
            }
            cfg.getTemplate("service-impl.ftl").process(data,
                    new FileWriter(file1));
            System.out.println(modelNameUpperCamel + "ServiceImpl.java 生成成功");
        } catch (Exception e) {
            throw new RuntimeException("生成Service失败", e);
        }
    }

    private static void genController(String tableName, String modelName) {
        try {
            freemarker.template.Configuration cfg = getConfiguration();

            Map<String, Object> data = new HashMap<>();
            data.put("AUTHOR", AUTHOR);
            data.put("CREATE", CREATE);
            String modelNameUpperCamel = StringUtils.isEmpty(modelName) ? tableNameConvertUpperCamel(tableName) : modelName;
            data.put("baseRequestMapping", modelNameConvertMappingPath(modelNameUpperCamel));
            data.put("modelNameUpperCamel", modelNameUpperCamel);
            data.put("modelNameLowerCamel", modelNameConvertLowerCamel(modelNameUpperCamel));
            data.put("basePackage", BASE_PACKAGE);

            File file = new File(PROJECT_PATH + JAVA_PATH + PACKAGE_PATH_CONTROLLER + modelNameUpperCamel + "Controller.java");
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            //cfg.getTemplate("controller-restful.ftl").process(data, new FileWriter(file));
            cfg.getTemplate("controller.ftl").process(data, new FileWriter(file));

            System.out.println(modelNameUpperCamel + "Controller.java 生成成功");
        } catch (Exception e) {
            throw new RuntimeException("生成Controller失败", e);
        }

    }

    private static freemarker.template.Configuration getConfiguration() throws IOException {
        freemarker.template.Configuration cfg = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_23);
        cfg.setDirectoryForTemplateLoading(new File(TEMPLATE_FILE_PATH));
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.IGNORE_HANDLER);
        return cfg;
    }

    private static String tableNameConvertLowerCamel(String tableName) {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, tableName.toLowerCase());
    }

    private static String tableNameConvertUpperCamel(String tableName) {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, tableName.toLowerCase());

    }

    private static String tableNameConvertMappingPath(String tableName) {
        tableName = tableName.toLowerCase();//兼容使用大写的表名
        return "/" + (tableName.contains("_") ? tableName.replaceAll("_", "/") : tableName);
    }

    private static String modelNameConvertMappingPath(String modelName) {
        String tableName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, modelName);
        return tableNameConvertMappingPath(tableName);
    }

    private static String packageConvertPath(String packageName) {
        return String.format("/%s/", packageName.contains(".") ? packageName.replaceAll("\\.", "/") : packageName);
    }

    private static String modelNameConvertLowerCamel(String modelName){
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, modelName);
    }

}