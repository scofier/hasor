/*
 * Copyright 2008-2009 the original author or authors.
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
package org.more.hypha.context;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.more.NoDefinitionException;
import org.more.hypha.AbstractBeanDefine;
import org.more.hypha.ApplicationContext;
import org.more.hypha.ELContext;
import org.more.hypha.Event;
import org.more.hypha.EventManager;
import org.more.hypha.ExpandPointManager;
import org.more.hypha.ScopeContext;
import org.more.hypha.commons.AbstractELContext;
import org.more.hypha.commons.AbstractScopeContext;
import org.more.hypha.commons.engine.EngineLogic;
import org.more.util.attribute.IAttribute;
/**
 * 简单的{@link ApplicationContext}接口实现类，该类只是提供了一个平台。
 * Date : 2011-4-8
 * @author 赵永春 (zyc@byshell.org)
 */
public abstract class AbstractApplicationContext implements ApplicationContext {
    private PropxyClassLoader     classLoader     = null;
    //init期间必须构建的六大基础对象
    private Object                contextObject   = null;
    private AbstractELContext     elContext       = null;
    private AbstractScopeContext  scopeContext    = null;
    //
    private Map<String, Object>   singleBeanCache = null;
    private Map<String, Class<?>> singleTypeCache = null;
    //
    private EngineLogic           engineLogic     = null;
    /*------------------------------------------------------------*/
    public AbstractApplicationContext(ClassLoader classLoader) {
        this.classLoader = new PropxyClassLoader();
        this.classLoader.setLoader(classLoader);
    };
    public Object getContextObject() {
        return this.contextObject;
    };
    public void setContextObject(Object contextObject) {
        this.contextObject = contextObject;
    };
    public EventManager getEventManager() {
        return this.getBeanResource().getEventManager();
    };
    public ExpandPointManager getExpandPointManager() {
        return this.getBeanResource().getExpandPointManager();
    };
    public ELContext getELContext() {
        return this.elContext;
    };
    public ScopeContext getScopeContext() {
        return this.scopeContext;
    };
    public ClassLoader getBeanClassLoader() {
        return this.classLoader;
    };
    /**替换当前的ClassLoader。*/
    public void setBeanClassLoader(ClassLoader loader) {
        this.classLoader.setLoader(loader);
    };
    public EngineLogic getEngineLogic() {
        return this.engineLogic;
    };
    /*------------------------------------------------------------*/
    /**清理掉{@link AbstractApplicationContext}对象中所缓存的单例Bean对象。*/
    public void clearSingleBean() {
        this.singleBeanCache.clear();
    };
    /**获取一个int该int表示了{@link AbstractApplicationContext}对象中已经缓存了的单例对象数目。*/
    public int getCacheBeanCount() {
        return this.singleBeanCache.size();
    };
    public abstract AbstractDefineResource getBeanResource();
    /**在init期间被调用，子类可以重写它用来替换EL上下文。*/
    protected AbstractELContext createELContext() {
        return new AbstractELContext() {};
    };
    /**在init期间被调用，子类可以重写它用来替换作用域管理器。*/
    protected AbstractScopeContext createScopeContext() {
        return new AbstractScopeContext() {};
    };
    /**该方法可以获取{@link AbstractApplicationContext}接口对象所使用的属性管理器。子类可以通过重写该方法以来控制属性管理器对象。*/
    protected IAttribute getAttribute() {
        return this.getBeanResource();
    };
    /**获取Flash，这个flash是一个内部信息携带体。它可以贯穿整个hypha的所有阶段。而且不受跨线程限制。*/
    public IAttribute getFlash() {
        return this.getBeanResource().getFlash();
    };
    /**获取Flash，这个flash是一个内部信息携带体。它可以贯穿整个hypha的所有阶段。但是这个FLASH受跨线程限制。*/
    public IAttribute getThreadFlash() {
        return this.getBeanResource().getThreadFlash();
    };
    /*------------------------------------------------------------*/
    public void init() throws Throwable {
        this.elContext = this.createELContext();
        this.scopeContext = this.createScopeContext();
        //
        this.singleBeanCache = new HashMap<String, Object>();
        this.singleTypeCache = new HashMap<String, Class<?>>();
        //
        this.engineLogic = new EngineLogic();
        //
        this.elContext.init(this);
        this.scopeContext.init(this);
        this.engineLogic.init(this);
        //
        this.getEventManager().doEvent(Event.getEvent(InitEvent.class), this);
    };
    /**当JVM回收该对象时自动调用销毁方法。*/
    protected void finalize() throws Throwable {
        try {
            this.destroy();
        } catch (Exception e) {}
        super.finalize();
    };
    public void destroy() throws Throwable {
        /**销毁事件*/
        this.getEventManager().doEvent(Event.getEvent(DestroyEvent.class), this);
        this.getEventManager().popEvent();//弹出所有事件
        //
        this.elContext = null;
        this.scopeContext = null;
        //
        this.singleBeanCache = null;
        this.singleTypeCache = null;
        //
        this.engineLogic = null;
    };
    public <T> T getBean(String defineID, Object... objects) throws Throwable {
        //-------------------------------------------------------------------检查单态
        if (this.singleBeanCache.containsKey(defineID) == true)
            return (T) this.singleBeanCache.get(defineID);
        //-------------------------------------------------------------------获取
        final String KEY = "GETBEAN_PARAM";
        try {
            this.getThreadFlash().setAttribute(KEY, objects);
            AbstractBeanDefine define = this.getBeanDefinition(defineID);
            if (define == null)
                throw new NoDefinitionException("不存在id为[" + defineID + "]的Bean定义。");
            //1.获取bean以及bean类型。
            Class<?> beanType = this.getBeanType(defineID, objects);//获取类型
            Object bean = this.engineLogic.builderBean(define, objects);//生成Bean
            //3.单态缓存&类型匹配
            bean = this.cast(beanType, bean);
            if (define.isSingleton() == true)
                this.singleBeanCache.put(defineID, bean);
            return (T) bean;
        } catch (Throwable e) {
            throw e;
        } finally {
            this.getThreadFlash().removeAttribute(KEY);
        }
    };
    /**检测对象类型是否匹配定义类型，如果没有指定beanType参数则直接返回。*/
    private Object cast(Class<?> beanType, Object obj) throws ClassCastException {
        if (beanType != null)
            return beanType.cast(obj);
        return obj;
    };
    public Class<?> getBeanType(String defineID, Object... objects) throws Throwable {
        //-------------------------------------------------------------------检查单态
        if (this.singleTypeCache.containsKey(defineID) == true)
            return this.singleTypeCache.get(defineID);
        //-------------------------------------------------------------------获取
        final String KEY = "GETBEAN_PARAM";
        try {
            this.getThreadFlash().setAttribute(KEY, objects);
            AbstractBeanDefine define = this.getBeanDefinition(defineID);
            if (define == null)
                throw new NoDefinitionException("不存在id为[" + defineID + "]的Bean定义。");
            Class<?> beanType = this.engineLogic.builderType(define, objects);
            if (define.isSingleton() == true)
                this.singleTypeCache.put(defineID, beanType);
            return beanType;
        } catch (Throwable e) {
            throw e;
        } finally {
            this.getThreadFlash().removeAttribute(KEY);
        }
    };
    public List<String> getBeanDefinitionIDs() {
        return this.getBeanResource().getBeanDefinitionIDs();
    };
    public AbstractBeanDefine getBeanDefinition(String id) throws NoDefinitionException {
        return this.getBeanResource().getBeanDefine(id);
    };
    public boolean containsBean(String id) {
        return this.getBeanResource().containsBeanDefine(id);
    };
    public boolean isPrototype(String id) throws NoDefinitionException {
        return this.getBeanResource().isPrototype(id);
    };
    public boolean isSingleton(String id) throws NoDefinitionException {
        return this.getBeanResource().isSingleton(id);
    };
    public boolean isFactory(String id) throws NoDefinitionException {
        return this.getBeanResource().isFactory(id);
    };
    public boolean isTypeMatch(String id, Class<?> targetType) throws Throwable {
        //Object.class.isAssignableFrom(XmlTest.class); return true;
        if (targetType == null)
            throw new NullPointerException("参数targetType不能为空.");
        Class<?> beanType = this.getBeanType(id);
        return targetType.isAssignableFrom(beanType);
    };
    /*------------------------------------------------------------*/
    public boolean contains(String name) {
        return this.getAttribute().contains(name);
    };
    public void setAttribute(String name, Object value) {
        this.getAttribute().setAttribute(name, value);
    };
    public Object getAttribute(String name) {
        return this.getAttribute().getAttribute(name);
    };
    public void removeAttribute(String name) {
        this.getAttribute().removeAttribute(name);
    };
    public String[] getAttributeNames() {
        return this.getAttribute().getAttributeNames();
    };
    public void clearAttribute() {
        this.getAttribute().clearAttribute();
    };
    public Map<String, Object> toMap() {
        return this.getAttribute().toMap();
    };
};