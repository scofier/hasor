/*
 * Copyright 2008-2009 the original 赵永春(zyc@hasor.net).
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
package net.hasor.rsf.center.core.rsf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.hasor.core.ApiBinder;
import net.hasor.core.AppContext;
import net.hasor.core.LifeModule;
import net.hasor.rsf.bootstrap.RsfModule;
import net.hasor.rsf.center.domain.constant.RsfCenterCfg;
/**
 * RSF 通信端口配置
 * 
 * @version : 2016年2月4日
 * @author 赵永春(zyc@hasor.net)
 */
public class RsfInit implements LifeModule {
    protected Logger     logger       = LoggerFactory.getLogger(getClass());
    private RsfCenterCfg rsfCenterCfg = null;
    public RsfInit(RsfCenterCfg rsfCenterCfg) {
        this.rsfCenterCfg = rsfCenterCfg;
    }
    @Override
    public void loadModule(ApiBinder apiBinder) throws Throwable {
        String ns = "http://project.hasor.net/hasor/schema/main";
        apiBinder.getEnvironment().getSettings().setSetting("hasor.rsfConfig.port", rsfCenterCfg.getRsfPort(), ns);
        apiBinder.installModule(new RsfModule());
    }
    @Override
    public void onStart(AppContext appContext) throws Throwable {
        // TODO Auto-generated method stub
    }
    @Override
    public void onStop(AppContext appContext) throws Throwable {
        // TODO Auto-generated method stub
    }
}