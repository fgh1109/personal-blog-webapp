package com.zuoxiaolong.util;

import com.zuoxiaolong.algorithm.Match;
import com.zuoxiaolong.algorithm.Random;
import com.zuoxiaolong.config.Configuration;
import com.zuoxiaolong.dao.ArticleDao;
import com.zuoxiaolong.dao.MatchDao;
import com.zuoxiaolong.model.ViewMode;
import freemarker.template.Template;
import org.apache.log4j.Logger;

import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author 左潇龙
 * @since 2015年5月24日 上午2:04:43
 */
public abstract class FreemarkerUtil {

    private static final Logger logger = Logger.getLogger(FreemarkerUtil.class);

    private static final String DEFAULT_NAMESPACE = "blog";

    private static final int DEFAULT_RIGHT_ARTICLE_NUMBER = 5;

    public static Map<String, Object> buildCommonDataMap(ViewMode viewMode) {
        return buildCommonDataMap(DEFAULT_NAMESPACE, viewMode);
    }

    public static Map<String, Object> buildCommonDataMap(String namespace, ViewMode viewMode) {
        Map<String, Object> data = new HashMap<>();
        data.put("contextPath", Configuration.isProductEnv() ? Configuration.get("context.path.product") : Configuration.get("context.path"));
        if (ViewMode.DYNAMIC == viewMode) {
            data.put("allArticlesUrl", "/blog/article_list.ftl?current=1");
            data.put("indexUrl", "/blog/index.ftl");
        } else {
            data.put("allArticlesUrl", "/html/article_list_1.html");
            data.put("indexUrl", "/");
        }
        if (namespace.equals("blog")) {
            List<Map<String, String>> articleList = ArticleDao.getArticles("create_date", viewMode);
            List<Map<String, String>> articleListCopy = new ArrayList<>(articleList);
            data.put("accessCharts",ArticleDao.getArticles("access_times", viewMode));
            data.put("newCharts",articleList);
            data.put("recommendCharts",ArticleDao.getArticles("good_times", viewMode));
            data.put("imageArticles",Random.random(articleListCopy, DEFAULT_RIGHT_ARTICLE_NUMBER));
        }
        if (namespace.equals("dota")) {
            List<Map<String, String>> matchList = MatchDao.getAll();
            List<Map<String, Object>> hotCharts = new ArrayList<Map<String,Object>>();
            List<Map<String, Object>> winCharts = new ArrayList<Map<String,Object>>();
            List<Map<String, Object>> winTimesCharts = new ArrayList<Map<String,Object>>();
            Map<String, int[]> resultCountMap = Match.computeHeroCharts(matchList);
            Match.fillHeroCharts(resultCountMap, hotCharts, winCharts, winTimesCharts);
            data.put("hotCharts", hotCharts);
            data.put("winCharts", winCharts);
            data.put("winTimesCharts", winTimesCharts);
            data.put("totalCount", MatchDao.count());
        }
        return data;
    }

    public static void generate(String template, Writer writer, ViewMode viewMode) {
        generate(DEFAULT_NAMESPACE, template, writer, buildCommonDataMap(viewMode));
    }

    public static void generate(String template, Writer writer, Map<String, Object> data) {
        generate(DEFAULT_NAMESPACE, template, writer, data);
    }

    public static void generate(String namespace, String template, Writer writer, ViewMode viewMode) {
        generate(namespace, template, writer, buildCommonDataMap(namespace, viewMode));
    }

    public static void generate(String namespace, String template, Writer writer, Map<String, Object> data) {
        generateByTemplatePath(namespace + "/" + template + ".ftl", writer, data);
    }

    public static void generateByTemplatePath(String templatePath, Writer writer, ViewMode viewMode) {
        generateByTemplatePath(templatePath, writer, buildCommonDataMap(getNamespace(templatePath), viewMode));
    }

    public static void generateByTemplatePath(String templatePath, Writer writer, Map<String, Object> data) {
        try {
            Template htmlTemplate = Configuration.getFreemarkerConfiguration().getTemplate(templatePath);
            htmlTemplate.process(data, writer);
        } catch (Exception e) {
            logger.error("get template failed for templatePath:" + templatePath, e);
            throw new RuntimeException(e);
        }
    }

    public static String getNamespace(String templatePath) {
        templatePath = replaceStartSlant(templatePath);
        return templatePath.substring(0, templatePath.indexOf("/"));
    }

    public static String replaceStartSlant(String s) {
        while (s.startsWith("/")) {
            s = s.substring(1);
        }
        return s;
    }

}