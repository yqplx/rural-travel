package com.rural.travel.guide;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rural.travel.entity.User;
import com.rural.travel.guide.dto.ContentBatchDTO;
import com.rural.travel.guide.entity.KgCounty;
import com.rural.travel.guide.entity.KgEntity;
import com.rural.travel.guide.entity.KgRelation;
import com.rural.travel.guide.entity.VisitorProfile;
import com.rural.travel.guide.mapper.KgCountyMapper;
import com.rural.travel.guide.mapper.KgEntityMapper;
import com.rural.travel.guide.mapper.KgRelationMapper;
import com.rural.travel.guide.mapper.MmContentMapper;
import com.rural.travel.guide.mapper.VisitorProfileMapper;
import com.rural.travel.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 方案三演示数据初始化：一个县域历史文化古镇，12 景点 / 6 非遗 / 8 美食。
 *
 * <p>按"表为空才写入"幂等执行。内容为演示数据，县域与资源名称为虚构，
 * 用于完整跑通 S1~S6 链路；正式部署时清空即可录入真实县域资源。</p>
 */
@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class GuideDataInitializer implements ApplicationRunner {

    private static final String COUNTY_CODE = "ZJ-YX-01";
    private static final String COUNTY_NAME = "云溪县";

    private final KgCountyMapper countyMapper;
    private final KgEntityMapper entityMapper;
    private final KgRelationMapper relationMapper;
    private final VisitorProfileMapper profileMapper;
    private final UserMapper userMapper;
    private final MmContentMapper contentMapper;
    private final MultimodalContentService contentService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            seedCounty();
            seedEntities();
            seedRelations();
            seedContents();
            seedProfiles();
        } catch (Exception e) {
            // 演示数据失败不阻断主系统启动
            log.warn("方案三演示数据初始化跳过: {}", e.getMessage());
        }
    }

    private record Seed(String type, String name, String summary, String detail, String tags,
                       double lng, double lat, int minutes, double crowd, double heat, String open) {
    }

    private static final List<Seed> SEEDS = List.of(
            // ---------------- 12 景点 ----------------
            new Seed("SCENIC", "梅城古镇城门",
                    "云溪县城的门户，明代嘉靖年间重修的砖石城门，门额上「梅城」二字为旧时知县手书。",
                    "城门始建于明洪武年间，现存墙体为嘉靖三十六年重修，条石基座、青砖券门，门洞深九米。旧时是商旅进出徽杭古道的必经之处，城门内侧仍留有拴马石与更房遗址。",
                    "古村落,历史建筑,摄影", 119.7200, 29.3500, 25, 0.75, 0.90, "全天开放"),
            new Seed("SCENIC", "明清一条街",
                    "全长八百米的青石板老街，两侧是连片的明清店铺与作坊，前店后坊的格局保存至今。",
                    "老街自城门向北延伸，鼎盛时有商号一百二十余家。如今保留了二十余幢木结构二层店铺，剃头店、铁匠铺、酱园仍在营业，是县域里最有烟火气的一段路。",
                    "古村落,历史建筑,美食,摄影", 119.7212, 29.3508, 45, 0.80, 0.88, "全天开放"),
            new Seed("SCENIC", "云溪书院",
                    "清乾隆年间创办的县域最高学府，讲堂、藏书楼与泮池完整，院中两株百年丹桂。",
                    "书院历一百八十年，共出进士七人、举人三十余人。现存讲堂三进，梁架为抬梁式，木雕雀替保存完好。藏书楼旧藏典籍三千余册，现存部分移交县图书馆。",
                    "历史建筑,古村落,摄影", 119.7190, 29.3515, 35, 0.35, 0.70, "08:30-17:00"),
            new Seed("SCENIC", "望江楼",
                    "临江而立的三层木构楼阁，登楼可俯瞰云溪与对岸的梯田茶山。",
                    "望江楼原为清代盐商会所，因临江得名。三层四面开窗，晴日可远眺二十里。楼内现存碑刻四方，记录历年修葺与水文，是研究县域水系变迁的实物资料。",
                    "历史建筑,摄影,观景", 119.7225, 29.3492, 25, 0.45, 0.66, "08:00-18:00"),
            new Seed("SCENIC", "古码头遗址",
                    "明清时期云溪水运的起点，青石台阶直入江中，岸边仍存系船石与碑刻。",
                    "旧时山货、竹木、茶叶经此装船下杭嘉湖，回程运回布匹与海货。码头现存石阶四十八级，水位低时可见当年拴船的铸铁环。",
                    "历史建筑,古村落", 119.7231, 29.3485, 20, 0.30, 0.58, "全天开放"),
            new Seed("SCENIC", "文昌阁",
                    "供祀文昌帝君的六角三层阁楼，旧时科举学子考前必来祈福。",
                    "阁高十六米，攒尖顶，檐角悬铜铃。一层内壁嵌有历年科甲题名碑，字迹多已漫漶，仍可辨清嘉庆至光绪年间二十余名举人姓名。",
                    "历史建筑,古村落", 119.7185, 29.3495, 20, 0.40, 0.62, "08:30-17:00"),
            new Seed("SCENIC", "后山观景台",
                    "城北后山的制高点，清晨可观云海翻涌，傍晚能看炊烟次第升起。",
                    "步行上山约二十分钟，沿途是茶园与竹林。观景台为木栈道结构，视野开阔，是拍摄古镇全景与日出的最佳位置。",
                    "摄影,徒步,茶园,竹海", 119.7160, 29.3525, 40, 0.25, 0.60, "全天开放"),
            new Seed("SCENIC", "千亩荷塘",
                    "城东连片荷塘，盛夏时接天莲叶，塘中有木栈道与观荷亭。",
                    "荷塘历来种植子莲与藕莲，夏季采莲、秋季挖藕。塘中栈道全长一千米，可行至中央的观荷亭。七八月花期最佳，清晨带露时最出片。",
                    "摄影,亲子,田园风光", 119.7250, 29.3530, 40, 0.50, 0.72, "全天开放"),
            new Seed("SCENIC", "古樟树群",
                    "村口七株百年古樟，最大的一株胸径一米八，树冠覆盖半亩。",
                    "古樟为村中风水林，旧时是村民议事与纳凉之处。树下有青石凳与古井，夏天温度比村外低两三度。",
                    "古村落,徒步", 119.7175, 29.3520, 20, 0.20, 0.55, "全天开放"),
            new Seed("SCENIC", "溪畔花海",
                    "云溪南岸的季节性花田，春油菜、夏向日葵、秋粉黛乱子草。",
                    "花田由村民流转土地统一种植，一年三季换茬。田埂间留了拍照道与观景台，不收门票，是县域近年最出圈的打卡点。",
                    "摄影,亲子,田园风光,油菜花", 119.7268, 29.3542, 35, 0.55, 0.68, "全天开放"),
            new Seed("SCENIC", "石拱桥群",
                    "云溪支流上五座明清石拱桥，形态各异，最短的三米，最长的十八米。",
                    "五桥沿溪一公里内依次排开，分别建于明万历、清康熙、乾隆、嘉庆与道光年间，是研究县域桥梁营造法式的连续样本。",
                    "古村落,历史建筑,摄影", 119.7205, 29.3480, 25, 0.38, 0.64, "全天开放"),
            new Seed("SCENIC", "乡愁记忆馆",
                    "由旧粮仓改造的县域农耕记忆馆，收藏农具、契约与老照片两千余件。",
                    "馆藏以村民捐赠为主，按春耕、夏耘、秋收、冬藏四个单元陈列。二楼有口述史放映区，可看二十位老人的访谈影像。",
                    "古村落,亲子,历史建筑", 119.7195, 29.3502, 30, 0.28, 0.52, "09:00-16:30"),

            // ---------------- 6 非遗 ----------------
            new Seed("HERITAGE", "云溪竹编",
                    "以本地三年生毛竹为原料的编织技艺，成品细密如绢，可盛水不漏。",
                    "云溪竹编有据可考已传六代，编织前需将竹材剖成十六层篾丝，最细的篾丝仅一毫米。代表作品为竹丝扣瓷茶具与六角提篮，编织一只提篮需两万余次穿插。现有省级代表性传承人一人，县级三人。",
                    "非遗,手作", 119.7218, 29.3510, 30, 0.40, 0.66, "09:00-17:00"),
            new Seed("HERITAGE", "梅城剪纸",
                    "不用稿样的徒手剪纸，题材多为戏曲人物与岁时节令。",
                    "传承人下剪前不打草稿，凭记忆与手感一次成形，讲究「以刀代笔、连而不断」。常见题材为《西厢记》人物、十二生肖与二十四节气图。春节前后，家家户户贴剪纸窗花的习俗仍在。",
                    "非遗,手作,亲子", 119.7215, 29.3505, 25, 0.45, 0.62, "09:00-17:00"),
            new Seed("HERITAGE", "古法榨油",
                    "木榨山茶油的完整工序，从焙炒、碾粉、蒸粉到撞榨，全凭人力与经验。",
                    "榨坊现存百年木榨一具，撞榨时需两人配合，反复撞击木楔三百余次，出油率约百分之二十五。整套工序保留「看色、闻香、试温」三项经验判断，无法完全由机械替代。",
                    "非遗,手作,美食", 119.7222, 29.3516, 30, 0.30, 0.58, "09:00-17:00"),
            new Seed("HERITAGE", "云溪米酒酿造",
                    "以本地糯米与山泉水酿造的低度黄酒，酒曲为自制草本曲。",
                    "酒曲采端午前后的辣蓼草与米粉制成，发酵七日后蒸饭拌曲，入缸冬酿春出。酒色琥珀，甜中带微酸，是县域红白喜事的必备。酿造周期与节气强相关，一年只做两季。",
                    "非遗,美食,手作", 119.7208, 29.3512, 35, 0.35, 0.64, "09:00-17:00"),
            new Seed("HERITAGE", "木版年画",
                    "梨木刻版、手工套色的年画印制技艺，一套门神需五块版五次套印。",
                    "刻版用本地梨木，一块版可用数十年。印制时先墨线后套色，对版全凭手感与标记线，误差需控制在一毫米内。现存老版四十余块，最早的刻于清光绪年间。",
                    "非遗,手作,摄影", 119.7212, 29.3501, 25, 0.42, 0.56, "09:00-17:00"),
            new Seed("HERITAGE", "舞龙灯",
                    "正月巡游的板凳龙，每节板凳由一户人家承担，最长时达一百零八节。",
                    "板凳龙以长条板凳首尾相接，每节点烛，龙头由村中长者掌持。巡游路线固定绕村三圈，寓意风调雨顺。龙身节数随村中户数增减，是观察村落人口变迁的活标本。",
                    "非遗,摄影,古村落", 119.7228, 29.3508, 40, 0.60, 0.70, "正月十三至十八"),

            // ---------------- 8 美食 ----------------
            new Seed("FOOD", "梅城酥饼",
                    "炭火烘烤的梅干菜猪肉酥饼，十六层酥皮，出炉十分钟风味最佳。",
                    "面皮与油酥反复擀折十六次形成层状，馅料为梅干菜与土猪肉末，贴炉壁炭烤。刚出炉时酥皮一碰就掉渣，凉了回炉两分钟可恢复大半口感。",
                    "美食,古村落", 119.7210, 29.3506, 20, 0.65, 0.80, "07:00-19:00"),
            new Seed("FOOD", "云溪米酒",
                    "琥珀色的低度糯米酒，甜中带微酸，温热后配桂花更佳。",
                    "冬酿春出，酒精度约十二度。可冷饮可温饮，冬天加姜丝与红糖煮开，是当地人驱寒的老法子。散装按斤卖，也有陶坛装可带走。",
                    "美食,非遗", 119.7206, 29.3509, 20, 0.40, 0.72, "09:00-20:00"),
            new Seed("FOOD", "溪鱼一锅鲜",
                    "云溪野生小鱼与豆腐同炖，汤色奶白，只放盐与紫苏去腥。",
                    "鱼为溪中石斑与白条，现捞现做。铁锅烧热后先煎鱼再冲入开水，大火滚出奶白汤，最后下本地卤水豆腐。紫苏是灵魂，缺了就有土腥味。",
                    "美食,农家乐", 119.7228, 29.3488, 50, 0.55, 0.78, "11:00-14:00,17:00-20:30"),
            new Seed("FOOD", "艾草青团",
                    "清明前后的时令点心，艾草汁和糯米粉，豆沙或笋干咸馅。",
                    "艾草取野外嫩芽，焯水打成汁，与糯米粉揉成青色面团。甜馅为自制豆沙，咸馅是笋干肉末。清明前后各家各户都做，邻里互送。",
                    "美食,亲子,手作", 119.7213, 29.3503, 15, 0.50, 0.66, "07:30-18:00"),
            new Seed("FOOD", "手打麻糍",
                    "现打现卖的糯米麻糍，裹芝麻糖或黄豆粉，看打糍比吃还热闹。",
                    "蒸熟的糯米倒入石臼，两人轮流抡木槌捶打，打到不见米粒、能拉出长丝为止。打好的糍团趁热揪小块，滚上芝麻白糖或黄豆粉。",
                    "美食,手作,亲子", 119.7216, 29.3511, 20, 0.48, 0.68, "08:00-18:00"),
            new Seed("FOOD", "笋干老鸭煲",
                    "散养两年老鸭与笋干慢炖三小时，汤清味厚，是县域待客的头道菜。",
                    "老鸭先焯水去腥，与泡发的笋干、火腿脚圈同入砂锅，小火三小时不加多余调料。笋干吸饱鸭油，比鸭肉还抢手。",
                    "美食,农家乐", 119.7219, 29.3507, 45, 0.45, 0.74, "11:00-14:00,17:00-20:30"),
            new Seed("FOOD", "桂花糖藕",
                    "糯米灌入藕孔与红糖桂花同煮，切片后拉丝，是秋冬的甜口招牌。",
                    "选七孔老藕，糯米提前泡四小时后灌入孔中，用竹签封口，与红糖、干桂花同煮两小时。煮好放凉切片，藕丝相连。",
                    "美食,亲子", 119.7209, 29.3504, 15, 0.35, 0.60, "09:00-19:00"),
            new Seed("FOOD", "农家腊味拼盘",
                    "立冬后腌制的腊肉、腊肠与腊鸭，蒸熟切片，配米饭能吃两碗。",
                    "腊味挂在灶头上方，靠柴火烟熏两个月。好的腊肉肥肉透明、瘦肉枣红。拼盘通常三样各半斤，底下垫笋干或芋头吸油。",
                    "美食,农家乐", 119.7224, 29.3506, 40, 0.42, 0.70, "11:00-14:00,17:00-20:30")
    );

    private void seedCounty() {
        if (countyMapper.selectById(COUNTY_CODE) != null) return;
        KgCounty c = new KgCounty();
        c.setCode(COUNTY_CODE);
        c.setName(COUNTY_NAME);
        c.setProvince("浙江");
        c.setDescription("县域历史文化古镇演示数据：12 景点 / 6 非遗 / 8 美食，用于完整跑通 S1~S6 链路。");
        c.setStatus(1);
        countyMapper.insert(c);
        log.info("方案三：已初始化县域 {}", COUNTY_NAME);
    }

    private void seedEntities() {
        if (entityMapper.selectCount(new LambdaQueryWrapper<KgEntity>()
                .eq(KgEntity::getCountyCode, COUNTY_CODE)) > 0) return;
        for (Seed s : SEEDS) {
            KgEntity e = new KgEntity();
            e.setCountyCode(COUNTY_CODE);
            e.setEntityType(s.type());
            e.setName(s.name());
            e.setSummary(s.summary());
            e.setDetail(s.detail());
            e.setTags(s.tags());
            e.setLng(s.lng());
            e.setLat(s.lat());
            e.setVisitMinutes(s.minutes());
            e.setCrowd(s.crowd());
            e.setHeat(s.heat());
            e.setContentScore(0d);
            e.setOpenTime(s.open());
            e.setStatus(1);
            entityMapper.insert(e);
        }
        log.info("方案三：已初始化知识图谱实体 {} 个", SEEDS.size());
    }

    /**
     * 关系自动构建：
     * 景点之间 800 米内按实际步行时长建 TRANSFER(接驳) 关系，供式(2) 直接取用；
     * 非遗与美食按最近景点建 BELONGS_TO(隶属) 关系；同类非遗之间建 SAME_THEME。
     */
    private void seedRelations() {
        if (relationMapper.selectCount(null) > 0) return;
        List<KgEntity> all = entityMapper.selectList(
                new LambdaQueryWrapper<KgEntity>().eq(KgEntity::getCountyCode, COUNTY_CODE));
        List<KgEntity> scenics = all.stream().filter(e -> "SCENIC".equals(e.getEntityType())).toList();
        List<KgEntity> others = all.stream().filter(e -> !"SCENIC".equals(e.getEntityType())).toList();

        int n = 0;
        for (int i = 0; i < scenics.size(); i++) {
            for (int j = i + 1; j < scenics.size(); j++) {
                KgEntity a = scenics.get(i), b = scenics.get(j);
                double meters = GuideFormulas.distanceMeters(a.getLng(), a.getLat(), b.getLng(), b.getLat());
                if (meters > 800) continue;
                int walk = GuideFormulas.walkMinutes(meters, "normal");
                saveRelation(a.getId(), b.getId(), "TRANSFER", 1d, walk);
                n++;
            }
        }
        for (KgEntity o : others) {
            KgEntity nearest = null;
            double best = Double.MAX_VALUE;
            for (KgEntity s : scenics) {
                double d = GuideFormulas.distanceMeters(o.getLng(), o.getLat(), s.getLng(), s.getLat());
                if (d < best) {
                    best = d;
                    nearest = s;
                }
            }
            if (nearest == null) continue;
            saveRelation(o.getId(), nearest.getId(), "BELONGS_TO", 1d, GuideFormulas.walkMinutes(best, "normal"));
            saveRelation(nearest.getId(), o.getId(), "RECOMMEND_WITH", 0.8, GuideFormulas.walkMinutes(best, "normal"));
            n += 2;
        }
        List<KgEntity> heritages = all.stream().filter(e -> "HERITAGE".equals(e.getEntityType())).toList();
        for (int i = 0; i + 1 < heritages.size(); i++) {
            saveRelation(heritages.get(i).getId(), heritages.get(i + 1).getId(), "SAME_THEME", 0.6, 0);
            n++;
        }
        log.info("方案三：已初始化知识图谱关系 {} 条", n);
    }

    private void saveRelation(Long source, Long target, String type, double weight, int walk) {
        Long cnt = relationMapper.selectCount(new LambdaQueryWrapper<KgRelation>()
                .eq(KgRelation::getSourceId, source)
                .eq(KgRelation::getTargetId, target)
                .eq(KgRelation::getRelationType, type));
        if (cnt != null && cnt > 0) return;
        KgRelation r = new KgRelation();
        r.setSourceId(source);
        r.setTargetId(target);
        r.setRelationType(type);
        r.setWeight(weight);
        r.setWalkMinutes(walk);
        r.setStatus(1);
        relationMapper.insert(r);
    }

    /**
     * 内容量产：对每个实体批量生产「文案/语音稿/画面描述/视频脚本 × 三风格 × 普通话与方言」。
     * 演示数据强制走本地模板引擎（useAi=false），避免启动阶段消耗用户密钥额度。
     */
    private void seedContents() {
        if (contentMapper.selectCount(null) > 0) return;
        List<KgEntity> all = entityMapper.selectList(
                new LambdaQueryWrapper<KgEntity>().eq(KgEntity::getCountyCode, COUNTY_CODE));
        if (all.isEmpty()) return;

        ContentBatchDTO dto = new ContentBatchDTO();
        dto.setUseAi(false);
        int produced = 0;
        for (KgEntity e : all) {
            dto.setEntityId(e.getId());
            produced += contentService.produce(e.getId(), dto, null).getProduced();
        }
        log.info("方案三：已本地模板生产多模态内容 {} 条", produced);
    }

    /** 给体验账号预置画像，便于直接看到"千人千面"效果 */
    private void seedProfiles() {
        if (profileMapper.selectCount(null) > 0) return;
        User test = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, "test"));
        if (test == null) return;
        VisitorProfile p = new VisitorProfile();
        p.setUserId(test.getId());
        p.setAgeBand("青年");
        p.setGroupType("friends");
        p.setInterests("古村落,非遗,美食,摄影,历史建筑");
        p.setPace("slow");
        p.setPreferDialect("普通话");
        p.setDepthLevel(3);          // 青年深度游
        p.setPlanMinutes(240);       // 4 小时
        p.setAvoidCrowd(1);
        profileMapper.insert(p);
        log.info("方案三：已为体验账号预置「青年深度游」画像");
    }

    /** 便于外部扩展：返回演示县域编码 */
    public static String demoCountyCode() {
        return COUNTY_CODE;
    }

    /** 未被引用但保留给管理端「重置演示数据」按钮使用 */
    public List<String> demoEntityNames() {
        List<String> names = new ArrayList<>();
        for (Seed s : SEEDS) names.add(s.name());
        return names;
    }
}
