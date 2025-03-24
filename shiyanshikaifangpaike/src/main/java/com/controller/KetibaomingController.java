
package com.controller;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import com.service.TokenService;
import com.utils.*;
import java.lang.reflect.InvocationTargetException;

import com.service.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import com.annotation.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.entity.*;
import com.entity.view.*;
import com.service.*;
import com.utils.PageUtils;
import com.utils.R;
import com.alibaba.fastjson.*;

/**
 * 课题报名
 * 后端接口
 * @author
 * @email
*/
@RestController
@Controller
@RequestMapping("/ketibaoming")
public class KetibaomingController {
    private static final Logger logger = LoggerFactory.getLogger(KetibaomingController.class);

    private static final String TABLE_NAME = "ketibaoming";

    @Autowired
    private KetibaomingService ketibaomingService;


    @Autowired
    private TokenService tokenService;
    @Autowired
    private DictionaryService dictionaryService;

    //级联表非注册的service
    @Autowired
    private ShiyanshiYuyueService shiyanshiYuyueService;
    //注册表service
    @Autowired
    private YonghuService yonghuService;
    @Autowired
    private LaoshiService laoshiService;


    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永不会进入");
        else if("学生".equals(role))
            params.put("yonghuId",request.getSession().getAttribute("userId"));
        else if("老师".equals(role))
            params.put("laoshiId",request.getSession().getAttribute("userId"));
        CommonUtil.checkMap(params);
        PageUtils page = ketibaomingService.queryPage(params);

        //字典表数据转换
        List<KetibaomingView> list =(List<KetibaomingView>)page.getList();
        for(KetibaomingView c:list){
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(c, request);
        }
        return R.ok().put("data", page);
    }

    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        KetibaomingEntity ketibaoming = ketibaomingService.selectById(id);
        if(ketibaoming !=null){
            //entity转view
            KetibaomingView view = new KetibaomingView();
            BeanUtils.copyProperties( ketibaoming , view );//把实体数据重构到view中
            //级联表 学生
            //级联表
            YonghuEntity yonghu = yonghuService.selectById(ketibaoming.getYonghuId());
            if(yonghu != null){
            BeanUtils.copyProperties( yonghu , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "yonghuId"});//把级联的数据添加到view中,并排除id和创建时间字段,当前表的级联注册表
            view.setYonghuId(yonghu.getId());
            }
            //级联表 实验室预约
            //级联表
            ShiyanshiYuyueEntity shiyanshiYuyue = shiyanshiYuyueService.selectById(ketibaoming.getShiyanshiYuyueId());
            if(shiyanshiYuyue != null){
            BeanUtils.copyProperties( shiyanshiYuyue , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "yonghuId"});//把级联的数据添加到view中,并排除id和创建时间字段,当前表的级联注册表
            view.setShiyanshiYuyueId(shiyanshiYuyue.getId());
            }
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view, request);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 后端保存
    */
    @RequestMapping("/save")
    public R save(@RequestBody KetibaomingEntity ketibaoming, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,ketibaoming:{}",this.getClass().getName(),ketibaoming.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永远不会进入");
        else if("学生".equals(role))
            ketibaoming.setYonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));

        Wrapper<KetibaomingEntity> queryWrapper = new EntityWrapper<KetibaomingEntity>()
            .eq("shiyanshi_yuyue_id", ketibaoming.getShiyanshiYuyueId())
            .eq("yonghu_id", ketibaoming.getYonghuId())
            .eq("ketibaoming_zuoweihao", ketibaoming.getKetibaomingZuoweihao())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        KetibaomingEntity ketibaomingEntity = ketibaomingService.selectOne(queryWrapper);
        if(ketibaomingEntity==null){
            ketibaoming.setInsertTime(new Date());
            ketibaoming.setKetibaomingYesnoTypes(1);
            ketibaoming.setCreateTime(new Date());
            ketibaomingService.insert(ketibaoming);
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody KetibaomingEntity ketibaoming, HttpServletRequest request) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        logger.debug("update方法:,,Controller:{},,ketibaoming:{}",this.getClass().getName(),ketibaoming.toString());
        KetibaomingEntity oldKetibaomingEntity = ketibaomingService.selectById(ketibaoming.getId());//查询原先数据

        String role = String.valueOf(request.getSession().getAttribute("role"));
//        if(false)
//            return R.error(511,"永远不会进入");
//        else if("学生".equals(role))
//            ketibaoming.setYonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));
        //根据字段查询是否有相同数据
        Wrapper<KetibaomingEntity> queryWrapper = new EntityWrapper<KetibaomingEntity>()
            .notIn("id",ketibaoming.getId())
            .andNew()
            .eq("shiyanshi_yuyue_id", ketibaoming.getShiyanshiYuyueId())
            .eq("yonghu_id", ketibaoming.getYonghuId())
            .eq("insert_time", ketibaoming.getInsertTime())
            .eq("ketibaoming_shenhe_time", ketibaoming.getKetibaomingShenheTime())
            .eq("ketibaoming_zuoweihao", ketibaoming.getKetibaomingZuoweihao())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        KetibaomingEntity ketibaomingEntity = ketibaomingService.selectOne(queryWrapper);
        if(ketibaomingEntity==null){
            ketibaomingService.updateById(ketibaoming);//根据id更新
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }


    /**
    * 审核
    */
    @RequestMapping("/shenhe")
    public R shenhe(@RequestBody KetibaomingEntity ketibaomingEntity, HttpServletRequest request){
        logger.debug("shenhe方法:,,Controller:{},,ketibaomingEntity:{}",this.getClass().getName(),ketibaomingEntity.toString());

        KetibaomingEntity oldKetibaoming = ketibaomingService.selectById(ketibaomingEntity.getId());//查询原先数据

//        if(ketibaomingEntity.getKetibaomingYesnoTypes() == 2){//通过
//            ketibaomingEntity.setKetibaomingTypes();
//        }else if(ketibaomingEntity.getKetibaomingYesnoTypes() == 3){//拒绝
//            ketibaomingEntity.setKetibaomingTypes();
//        }
        ketibaomingEntity.setKetibaomingShenheTime(new Date());//审核时间
        ketibaomingService.updateById(ketibaomingEntity);//审核

        return R.ok();
    }

    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids, HttpServletRequest request){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        List<KetibaomingEntity> oldKetibaomingList =ketibaomingService.selectBatchIds(Arrays.asList(ids));//要删除的数据
        ketibaomingService.deleteBatchIds(Arrays.asList(ids));

        return R.ok();
    }


    /**
     * 批量上传
     */
    @RequestMapping("/batchInsert")
    public R save( String fileName, HttpServletRequest request){
        logger.debug("batchInsert方法:,,Controller:{},,fileName:{}",this.getClass().getName(),fileName);
        Integer yonghuId = Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId")));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            List<KetibaomingEntity> ketibaomingList = new ArrayList<>();//上传的东西
            Map<String, List<String>> seachFields= new HashMap<>();//要查询的字段
            Date date = new Date();
            int lastIndexOf = fileName.lastIndexOf(".");
            if(lastIndexOf == -1){
                return R.error(511,"该文件没有后缀");
            }else{
                String suffix = fileName.substring(lastIndexOf);
                if(!".xls".equals(suffix)){
                    return R.error(511,"只支持后缀为xls的excel文件");
                }else{
                    URL resource = this.getClass().getClassLoader().getResource("static/upload/" + fileName);//获取文件路径
                    File file = new File(resource.getFile());
                    if(!file.exists()){
                        return R.error(511,"找不到上传文件，请联系管理员");
                    }else{
                        List<List<String>> dataList = PoiUtil.poiImport(file.getPath());//读取xls文件
                        dataList.remove(0);//删除第一行，因为第一行是提示
                        for(List<String> data:dataList){
                            //循环
                            KetibaomingEntity ketibaomingEntity = new KetibaomingEntity();
//                            ketibaomingEntity.setShiyanshiYuyueId(Integer.valueOf(data.get(0)));   //实验室预约 要改的
//                            ketibaomingEntity.setYonghuId(Integer.valueOf(data.get(0)));   //用户 要改的
//                            ketibaomingEntity.setInsertTime(date);//时间
//                            ketibaomingEntity.setKetibaomingYesnoTypes(Integer.valueOf(data.get(0)));   //申请状态 要改的
//                            ketibaomingEntity.setKetibaomingYesnoText(data.get(0));                    //审核回复 要改的
//                            ketibaomingEntity.setKetibaomingShenheTime(sdf.parse(data.get(0)));          //审核时间 要改的
//                            ketibaomingEntity.setKetibaomingZuoweihao(data.get(0));                    //分配座位号 要改的
//                            ketibaomingEntity.setCreateTime(date);//时间
                            ketibaomingList.add(ketibaomingEntity);


                            //把要查询是否重复的字段放入map中
                        }

                        //查询是否重复
                        ketibaomingService.insertBatch(ketibaomingList);
                        return R.ok();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            return R.error(511,"批量插入数据异常，请联系管理员");
        }
    }





    /**
    * 前端列表
    */
    @IgnoreAuth
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("list方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));

        CommonUtil.checkMap(params);
        PageUtils page = ketibaomingService.queryPage(params);

        //字典表数据转换
        List<KetibaomingView> list =(List<KetibaomingView>)page.getList();
        for(KetibaomingView c:list)
            dictionaryService.dictionaryConvert(c, request); //修改对应字典表字段

        return R.ok().put("data", page);
    }

    /**
    * 前端详情
    */
    @RequestMapping("/detail/{id}")
    public R detail(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("detail方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        KetibaomingEntity ketibaoming = ketibaomingService.selectById(id);
            if(ketibaoming !=null){


                //entity转view
                KetibaomingView view = new KetibaomingView();
                BeanUtils.copyProperties( ketibaoming , view );//把实体数据重构到view中

                //级联表
                    YonghuEntity yonghu = yonghuService.selectById(ketibaoming.getYonghuId());
                if(yonghu != null){
                    BeanUtils.copyProperties( yonghu , view ,new String[]{ "id", "createDate"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setYonghuId(yonghu.getId());
                }
                //级联表
                    ShiyanshiYuyueEntity shiyanshiYuyue = shiyanshiYuyueService.selectById(ketibaoming.getShiyanshiYuyueId());
                if(shiyanshiYuyue != null){
                    BeanUtils.copyProperties( shiyanshiYuyue , view ,new String[]{ "id", "createDate"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setShiyanshiYuyueId(shiyanshiYuyue.getId());
                }
                //修改对应字典表字段
                dictionaryService.dictionaryConvert(view, request);
                return R.ok().put("data", view);
            }else {
                return R.error(511,"查不到数据");
            }
    }


    /**
    * 前端保存
    */
    @RequestMapping("/add")
    public R add(@RequestBody KetibaomingEntity ketibaoming, HttpServletRequest request){
        logger.debug("add方法:,,Controller:{},,ketibaoming:{}",this.getClass().getName(),ketibaoming.toString());
        String role = String.valueOf(request.getSession().getAttribute("role"));

        if(false)
            return R.error(511,"永远不会进入");
        else if("学生".equals(role))
            ketibaoming.setYonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));


        List<Integer> integers = new ArrayList<>();
        integers.add(1);
        integers.add(2);
        Wrapper<KetibaomingEntity> queryWrapper = new EntityWrapper<KetibaomingEntity>()
            .eq("shiyanshi_yuyue_id", ketibaoming.getShiyanshiYuyueId())
            .eq("yonghu_id", ketibaoming.getYonghuId())
            .in("ketibaoming_yesno_types", integers)
            ;
        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        KetibaomingEntity ketibaomingEntity = ketibaomingService.selectOne(queryWrapper);
        if(ketibaomingEntity==null){
            ketibaoming.setInsertTime(new Date());
            ketibaoming.setKetibaomingYesnoTypes(1);
            ketibaoming.setCreateTime(new Date());
            ketibaomingService.insert(ketibaoming);
            return R.ok();
        }else {
            if(ketibaomingEntity.getKetibaomingYesnoTypes()==1)
                return R.error(511,"有待审核的申请,请等待审核");
            else{
                return R.error(511,"已经通过审核了,不能重复审核");
            }
        }
    }

}
