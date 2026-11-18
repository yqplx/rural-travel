package com.rural.travel.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rural.travel.ai.ImageRecognitionService;
import com.rural.travel.entity.*;
import com.rural.travel.mapper.*;
import com.rural.travel.service.RatingService;
import com.rural.travel.service.ScenicSpotService;
import com.rural.travel.service.SysLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 首次启动自动初始化:分类/标签/景区/账号/虚拟样本/知识库/分析演示数据
 * 全部按"表为空才写入"幂等执行,可放心重复启动
 */
@Slf4j
@Component
@Order(2)
public class DataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final ScenicSpotMapper scenicSpotMapper;
    private final ScenicImageMapper scenicImageMapper;
    private final VirtualUserMapper virtualUserMapper;
    private final VirtualRatingMapper virtualRatingMapper;
    private final KnowledgeEntryMapper knowledgeEntryMapper;
    private final ScenicImageHashMapper scenicImageHashMapper;
    private final BrowseHistoryMapper browseHistoryMapper;
    private final PasswordEncoder passwordEncoder;
    private final RatingService ratingService;
    private final SysLogService sysLogService;
    private final ImageRecognitionService imageRecognitionService;

    public DataInitializer(UserMapper userMapper, CategoryMapper categoryMapper, TagMapper tagMapper,
                           ScenicSpotMapper scenicSpotMapper, ScenicImageMapper scenicImageMapper,
                           VirtualUserMapper virtualUserMapper, VirtualRatingMapper virtualRatingMapper,
                           KnowledgeEntryMapper knowledgeEntryMapper, ScenicImageHashMapper scenicImageHashMapper,
                           BrowseHistoryMapper browseHistoryMapper, PasswordEncoder passwordEncoder,
                           RatingService ratingService, SysLogService sysLogService,
                           ImageRecognitionService imageRecognitionService) {
        this.userMapper = userMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.scenicSpotMapper = scenicSpotMapper;
        this.scenicImageMapper = scenicImageMapper;
        this.virtualUserMapper = virtualUserMapper;
        this.virtualRatingMapper = virtualRatingMapper;
        this.knowledgeEntryMapper = knowledgeEntryMapper;
        this.scenicImageHashMapper = scenicImageHashMapper;
        this.browseHistoryMapper = browseHistoryMapper;
        this.passwordEncoder = passwordEncoder;
        this.ratingService = ratingService;
        this.sysLogService = sysLogService;
        this.imageRecognitionService = imageRecognitionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedDict();
        seedUsers();
        seedSpots();
        seedVirtual();
        seedKnowledge();
        seedImageHashes();
        seedBrowseHistory();
        refreshAllScores();
        // 异步预热真实封面图哈希(失败不影响启动)
        new Thread(imageRecognitionService::warmup).start();
        sysLogService.save(null, "system", "初始化", "基础数据初始化检查完成");
        log.info("========== 基础数据初始化检查完成 ==========");
    }

    // ==================== 分类与标签 ====================

    private void seedDict() {
        if (categoryMapper.selectCount(null) == 0) {
            String[][] cats = {
                    {"田园风光", "🌾"}, {"古镇村落", "🏘️"}, {"山水自然", "⛰️"},
                    {"民俗体验", "🎏"}, {"亲子农趣", "🧺"}, {"康养休闲", "♨️"}};
            int i = 1;
            for (String[] c : cats) {
                Category cat = new Category();
                cat.setName(c[0]);
                cat.setIcon(c[1]);
                cat.setSortOrder(i++);
                cat.setStatus(1);
                categoryMapper.insert(cat);
            }
        }
        if (tagMapper.selectCount(null) == 0) {
            String[] tags = {"油菜花", "梯田", "竹海", "茶园", "古村落", "民宿", "采摘", "农家乐",
                    "徒步", "摄影", "亲子", "温泉", "非遗", "美食", "骑行"};
            int i = 1;
            for (String t : tags) {
                Tag tag = new Tag();
                tag.setName(t);
                tag.setSortOrder(i++);
                tag.setStatus(1);
                tagMapper.insert(tag);
            }
        }
    }

    // ==================== 账号 ====================

    private void seedUsers() {
        if (userMapper.selectCount(null) > 0) return;
        addUser("admin", "admin123", "系统管理员", "ADMIN", "辽宁");
        addUser("test", "123456", "体验用户", "USER", "浙江");
        addUser("visitor01", "123456", "苏州游客", "USER", "江苏");
        addUser("visitor02", "123456", "上海游客", "USER", "上海");
        addUser("visitor03", "123456", "广州游客", "USER", "广东");
        addUser("visitor04", "123456", "北京游客", "USER", "北京");
        addUser("visitor05", "123456", "成都游客", "USER", "四川");
    }

    private void addUser(String username, String pwd, String nickname, String role, String province) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(pwd));
        u.setNickname(nickname);
        u.setRole(role);
        u.setStatus(1);
        u.setProvince(province);
        userMapper.insert(u);
    }

    // ==================== 景区 ====================

    private record SpotSeed(String name, String cat, String tags, String province, String city,
                            String address, String desc, String features, String tips,
                            double price, String openTime, String seed) {}

    private void seedSpots() {
        if (scenicSpotMapper.selectCount(null) > 0) return;
        List<SpotSeed> seeds = List.of(
                new SpotSeed("婺源江岭梯田花海", "田园风光", "油菜花,梯田,摄影", "江西", "上饶",
                        "江西省上饶市婺源县江岭村",
                        "江岭是婺源田园风光的代表,每到春天,十万亩梯田油菜花层层叠叠铺满山谷,白墙黛瓦的徽派村落点缀其间,宛如一幅流动的水墨画。清晨云雾缭绕时,花海若隐若现,是摄影爱好者心中的圣地。秋日稻谷金黄,又是另一番丰收景象。",
                        "万亩油菜花海,梯田观景长廊,徽派古村落",
                        "3-4月油菜花季人最多,建议工作日清晨前往;穿防滑鞋,田埂较窄。",
                        80, "07:30-17:30", "jiangling"),
                new SpotSeed("宏村水墨古村", "古镇村落", "古村落,民宿,摄影", "安徽", "黄山",
                        "安徽省黄山市黟县宏村镇",
                        "宏村始建于南宋,以牛形村落布局闻名,月沼如镜、南湖似镜,倒映着徽派建筑的白墙黛瓦。村中保存完好的明清民居上百幢,承志堂的木雕被誉为民间故宫。漫步青石板路,每一步都是画。",
                        "牛形水系村落,明清徽派民居,南湖画桥",
                        "清晨7点前进入可避开旅行团;村内民宿值得住一晚感受夜色。",
                        94, "全天开放", "hongcun"),
                new SpotSeed("安吉竹海茶乡", "山水自然", "竹海,茶园,徒步", "浙江", "湖州",
                        "浙江省湖州市安吉县天荒坪镇",
                        "安吉是中国著名的竹乡,六万亩大竹海连绵起伏,风过处竹浪翻滚,是《卧虎藏龙》的取景地。山间白茶茶园层层叠叠,清明前后可体验采茶炒茶。竹林步道负氧离子极高,是天然的大氧吧。",
                        "中国大竹海,安吉白茶体验,竹林徒步道",
                        "夏季避暑最佳;采茶体验需提前预约,记得带防晒帽。",
                        58, "08:00-17:00", "anji"),
                new SpotSeed("西江千户苗寨", "民俗体验", "非遗,美食,民宿", "贵州", "黔东南",
                        "贵州省黔东南苗族侗族自治州雷山县西江镇",
                        "西江千户苗寨是世界最大的苗族聚居村寨,上千座吊脚楼依山而建,层层叠叠直上山巅。入夜万家灯火亮起,仿佛银河落入山谷。苗族银饰、刺绣、长桌宴、芦笙舞,处处都是活着的民族文化。",
                        "千户吊脚楼夜景,苗族长桌宴,非遗银饰刺绣",
                        "夜景必看,建议住一晚;长桌宴高山流水敬酒量力而饮。",
                        90, "全天开放", "xijiang"),
                new SpotSeed("三圣花乡亲子农场", "亲子农趣", "采摘,亲子,农家乐", "四川", "成都",
                        "四川省成都市锦江区三圣街道",
                        "三圣花乡是成都人周末的后花园,四季花不断,采摘园、农家乐、手作工坊一家挨着一家。春天摘草莓、夏天赏荷塘、秋天打桂花、冬天烤红薯,孩子在这里能玩上一整天,大人也能找回久违的田园慢生活。",
                        "四季采摘,农事体验,花田打卡",
                        "周末人多,建议上午10点前到;采摘按斤计费,先问价再动手。",
                        0, "全天开放", "sansheng"),
                new SpotSeed("腾冲热海温泉乡", "康养休闲", "温泉,民宿", "云南", "保山",
                        "云南省保山市腾冲市热海景区",
                        "腾冲热海是著名的地热之乡,大滚锅沸水翻腾、蒸汽冲天,水温常年96度以上。泡一泡天然硫磺温泉,尝一串温泉煮的鸡蛋,再看一场火山地质奇观,是秋冬最治愈的打开方式。",
                        "热海大滚锅,天然硫磺温泉,火山地质奇观",
                        "温泉蛋在景区门口买更实惠;泡汤不宜超过15分钟一轮。",
                        60, "08:00-18:00", "tengchong"),
                new SpotSeed("元阳哈尼梯田", "田园风光", "梯田,摄影,徒步", "云南", "红河",
                        "云南省红河哈尼族彝族自治州元阳县新街镇",
                        "元阳哈尼梯田是哈尼族人一千三百年来雕刻大地的杰作,也是世界文化遗产。每年11月到次年4月,灌水期的梯田如千万面镜子镶嵌在山间,日出时金光万顷,多依树的云海日出被誉为中国最美日出之一。",
                        "世界文化遗产,灌水期镜面梯田,多依树日出",
                        "12月-次年2月灌水期最佳;看日出要5点前到观景台占位。",
                        70, "全天开放", "yuanyang"),
                new SpotSeed("周庄水乡古镇", "古镇村落", "古村落,美食,民宿", "江苏", "苏州",
                        "江苏省苏州市昆山市周庄镇",
                        "周庄有中国第一水乡之誉,井字形河道上横跨着十四座古桥,双桥因陈逸飞的油画闻名世界。乘一叶摇橹船穿行水巷,听船娘哼唱吴侬小调,再来一碗万三蹄,江南的温柔都在这里了。",
                        "双桥胜景,摇橹船游水巷,万三蹄美食",
                        "早上8点前和晚上9点后的古镇最安静;摇橹船可拼船更划算。",
                        88, "08:00-21:00", "zhouzhuang"),
                new SpotSeed("莫干山民宿集群", "康养休闲", "民宿,徒步,骑行", "浙江", "湖州",
                        "浙江省湖州市德清县莫干山镇",
                        "莫干山以竹、云、泉三胜闻名,两百多栋老别墅散落山间,如今化身精品民宿集群。白天骑行竹海绿道、徒步剑池飞瀑,晚上在山景露台上数星星,是江浙沪周末微度假的头号选择。",
                        "精品民宿集群,竹海骑行道,老别墅建筑群",
                        "民宿周末紧张,至少提前一周预订;山路弯多,新手司机慢行。",
                        0, "全天开放", "moganshan"),
                new SpotSeed("龙脊金坑梯田", "田园风光", "梯田,摄影", "广西", "桂林",
                        "广西壮族自治区桂林市龙胜各族自治县龙脊镇",
                        "龙脊梯田从山脚盘绕到山顶,小山如螺、大山成塔,气势磅礴。金坑大寨的红瑶风情浓郁,长发村的妇女头发长达一米多。秋天稻谷成熟时,整面山坡金光灿灿,是丰收最壮观的样子。",
                        "金坑大寨梯田,红瑶长发村,千层天梯",
                        "5月灌水、9-10月金黄两个时段最美;缆车上下省体力。",
                        80, "08:00-18:00", "longji"),
                new SpotSeed("凤凰古城苗寨风情", "民俗体验", "非遗,古村落,美食", "湖南", "湘西",
                        "湖南省湘西土家族苗族自治州凤凰县沱江镇",
                        "凤凰古城依沱江而建,吊脚楼临水而立,虹桥卧波,是沈从文笔下边城的原乡。白天逛古城九景、看苗族银饰锻造,夜晚沿江酒吧灯火阑珊。血粑鸭、酸汤鱼的味道,吃过就忘不掉。",
                        "沱江吊脚楼,沈从文故居,苗族银饰锻造",
                        "古城免费进入,九景联票按需购买;夜游沱江别有风味。",
                        0, "全天开放", "fenghuang"),
                new SpotSeed("崇明岛开心农场", "亲子农趣", "采摘,亲子,骑行", "上海", "上海",
                        "上海市崇明区中兴镇",
                        "崇明岛是上海的城市绿肺,开心农场里可以喂小羊、摘蔬菜、骑双人自行车环岛,江边湿地还能看到成群的候鸟。带上孩子来一天的乡野撒欢,回程再带一筐崇明老白酒和甜芦粟。",
                        "农场萌宠互动,环岛骑行,湿地观鸟",
                        "环岛骑行约2小时,注意防晒;秋季候鸟最多。",
                        30, "09:00-17:00", "chongming"));

        Map<String, Long> catMap = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getName, Category::getId));
        Map<String, Long> tagMap = tagMapper.selectList(null).stream()
                .collect(Collectors.toMap(Tag::getName, Tag::getId));

        for (SpotSeed seed : seeds) {
            ScenicSpot s = new ScenicSpot();
            s.setName(seed.name());
            s.setCategoryId(catMap.get(seed.cat()));
            List<Long> tagIds = Arrays.stream(seed.tags().split(","))
                    .map(t -> tagMap.get(t.trim())).filter(Objects::nonNull).toList();
            s.setTagIds(ScenicSpotService.wrapTagIds(tagIds));
            s.setProvince(seed.province());
            s.setCity(seed.city());
            s.setAddress(seed.address());
            s.setDescription(seed.desc());
            s.setFeatures(seed.features());
            s.setTips(seed.tips());
            s.setTicketPrice(BigDecimal.valueOf(seed.price()));
            s.setOpenTime(seed.openTime());
            s.setCoverImage("/images/rural-" + seed.seed() + ".jpg");
            s.setStatus(1);
            scenicSpotMapper.insert(s);
            for (int i = 1; i <= 3; i++) {
                ScenicImage img = new ScenicImage();
                img.setScenicSpotId(s.getId());
                img.setImageUrl("https://picsum.photos/seed/rural-" + seed.seed() + "-" + i + "/800/450");
                img.setSortOrder(i - 1);
                scenicImageMapper.insert(img);
            }
        }
    }

    // ==================== 虚拟用户与样本评分 ====================

    private void seedVirtual() {
        if (virtualUserMapper.selectCount(null) > 0) return;
        String[][] vusers = {
                {"山居客", "竹海,徒步", "常年泡在山里的徒步爱好者"},
                {"摄影老陈", "梯田,摄影", "追光逐影的退休摄影师"},
                {"亲子妈妈", "亲子,采摘", "每周末带娃放电的全职妈妈"},
                {"美食猎人", "美食,农家乐", "为一碗好菜可以开两百公里"},
                {"非遗爱好者", "非遗,古村落", "专注记录传统手工艺的研究生"},
                {"康养阿姨", "温泉,民宿", "注重养生的温泉常客"},
                {"骑行少年", "骑行,徒步", "一辆单车走乡野的大学生"},
                {"田园行者", "油菜花,梯田", "痴迷田园四季的自由职业者"}};
        Map<String, Long> tagMap = tagMapper.selectList(null).stream()
                .collect(Collectors.toMap(Tag::getName, Tag::getId));
        List<Long> vuIds = new ArrayList<>();
        for (String[] v : vusers) {
            VirtualUser vu = new VirtualUser();
            vu.setName(v[0]);
            List<Long> tagIds = Arrays.stream(v[1].split(","))
                    .map(t -> tagMap.get(t.trim())).filter(Objects::nonNull).toList();
            vu.setInterestTags(ScenicSpotService.wrapTagIds(tagIds));
            vu.setDescription(v[2]);
            virtualUserMapper.insert(vu);
            vuIds.add(vu.getId());
        }

        List<ScenicSpot> spots = scenicSpotMapper.selectList(null);
        Map<String, Long> spotByName = spots.stream()
                .collect(Collectors.toMap(ScenicSpot::getName, ScenicSpot::getId));
        // {虚拟用户序号, 景区名, 分数}
        Object[][] vr = {
                {0, "安吉竹海茶乡", 5}, {0, "莫干山民宿集群", 5}, {0, "元阳哈尼梯田", 4}, {0, "龙脊金坑梯田", 4},
                {1, "元阳哈尼梯田", 5}, {1, "龙脊金坑梯田", 5}, {1, "婺源江岭梯田花海", 5}, {1, "宏村水墨古村", 4},
                {2, "三圣花乡亲子农场", 5}, {2, "崇明岛开心农场", 5}, {2, "安吉竹海茶乡", 4}, {2, "腾冲热海温泉乡", 3},
                {3, "周庄水乡古镇", 4}, {3, "西江千户苗寨", 5}, {3, "凤凰古城苗寨风情", 4}, {3, "三圣花乡亲子农场", 4},
                {4, "西江千户苗寨", 5}, {4, "凤凰古城苗寨风情", 5}, {4, "宏村水墨古村", 4}, {4, "周庄水乡古镇", 3},
                {5, "腾冲热海温泉乡", 5}, {5, "莫干山民宿集群", 4}, {5, "安吉竹海茶乡", 4},
                {6, "莫干山民宿集群", 5}, {6, "崇明岛开心农场", 4}, {6, "安吉竹海茶乡", 4}, {6, "腾冲热海温泉乡", 3},
                {7, "婺源江岭梯田花海", 5}, {7, "龙脊金坑梯田", 4}, {7, "元阳哈尼梯田", 4}, {7, "宏村水墨古村", 3}, {7, "崇明岛开心农场", 4}};
        for (Object[] row : vr) {
            VirtualRating r = new VirtualRating();
            r.setVirtualUserId(vuIds.get((Integer) row[0]));
            r.setScenicSpotId(spotByName.get((String) row[1]));
            r.setScore((Integer) row[2]);
            virtualRatingMapper.insert(r);
        }
    }

    // ==================== AI 知识库(25条七大类) ====================

    private void seedKnowledge() {
        if (knowledgeEntryMapper.selectCount(null) > 0) return;
        String[][] kb = {
                {"景区门票怎么收费?", "各景区票价不同,详情页均有参考价;部分古镇村落免费开放,具体以景区当天公告为准。建议出行前在详情页确认票价与开放时间。", "门票,票价,收费,价格", "门票"},
                {"有学生票或优惠票吗?", "大部分景区对学生、老人、儿童有优惠政策,请携带有效证件到售票窗口咨询购买。", "学生票,优惠,老人,儿童,证件", "门票"},
                {"门票可以退吗?", "线上购票通常在未核销前支持退款,具体退改规则以购票页面说明为准。", "退票,退款,退改", "门票"},
                {"怎么去景区?交通方便吗?", "多数乡村旅游点建议自驾或包车前往;部分景区有旅游专线巴士。详情页的地址可直接用于导航。", "交通,怎么去,自驾,大巴,路线", "交通"},
                {"景区附近有停车场吗?", "主要景区均配有停车场,节假日车位紧张,建议早点出发或拼车前往。", "停车,停车场,车位", "交通"},
                {"从市区出发大概要多久?", "近郊景区一般1-2小时车程,远郊2-4小时,建议预留充足时间、错峰出行。", "多久,车程,距离,路程", "交通"},
                {"景区附近有住宿吗?", "乡村旅游点周边多有民宿、农家乐可供选择,旺季建议提前3-7天预订。", "住宿,民宿,酒店,住", "住宿"},
                {"民宿大概什么价位?", "乡村民宿平日100-300元/晚,节假日有所上浮,精品民宿500元以上,丰俭由人。", "民宿价格,房价,多少钱", "住宿"},
                {"当地有哪些特色美食?", "各地乡村美食不同:土鸡煲、笋干烧肉、手工豆腐、时令野菜都是常见招牌,景区详情页和当地农家乐都能尝到。", "美食,特色菜,吃什么,小吃", "美食"},
                {"景区里吃饭贵吗?", "农家乐人均40-80元比较常见,丰俭由人;也可以自带简餐野餐。", "吃饭,餐饮,贵吗,人均", "美食"},
                {"最佳旅游季节是什么时候?", "春季赏花(3-4月油菜花)、夏季避暑(6-8月竹海茶山)、秋季观景(9-11月梯田红叶)、冬季泡汤,四季皆有看头。", "季节,最佳时间,什么时候去", "行程"},
                {"一天能逛完吗?", "单个景区一般半天到一天;想深度体验建议住一晚,清晨和傍晚的乡村最有味道。", "一天,逛完,行程,玩多久", "行程"},
                {"适合带小孩去吗?", "亲子农趣类景区非常适合带娃:采摘、喂小动物、农事体验都很受欢迎。", "小孩,亲子,带娃,儿童", "行程"},
                {"下雨天适合去吗?", "细雨中的古镇村落别有韵味;但梯田、徒步类项目雨天路滑,建议改期或备好防滑装备。", "下雨,雨天,天气", "行程"},
                {"玩几天比较合适?", "周边2-3个景区串联,安排2天1晚最舒适;时间充裕可以3天2晚深度游。", "几天,安排,两日游", "行程"},
                {"当地有什么非遗文化体验?", "民俗体验类景区常有苗绣、扎染、竹编、打糍粑等非遗体验项目,可在景区列表筛选「民俗体验」分类查看。", "非遗,文化,体验,手工艺", "文化"},
                {"能体验农事活动吗?", "亲子农趣类景区四季都有农事体验:春插秧、夏采摘、秋收割、冬备年货,具体以景区活动安排为准。", "农事,采摘,体验,农活", "文化"},
                {"当地的民俗节庆有哪些?", "各地节庆不同,如苗年、丰收节等,出行前可在详情页或当地文旅公众号查询近期活动。", "节庆,民俗,活动,节日", "文化"},
                {"需要提前预约吗?", "大部分乡村景区无需预约;节假日热门景区可能限流,建议提前在官方渠道确认。", "预约,限流,预订", "通用"},
                {"景区开放时间是几点?", "各景区开放时间不同,详情页均有标注,一般为8:00-17:30,部分古镇全天开放。", "开放时间,几点,营业", "通用"},
                {"可以带宠物吗?", "多数户外景区允许宠物入内但需牵绳,室内场馆一般不允许,建议提前电话确认。", "宠物,带狗,带猫", "通用"},
                {"拍照好看吗?出片率高吗?", "梯田、花海、古村都是出片圣地,清晨和傍晚光线最佳;详情页游客评价里有不少实拍参考。", "拍照,出片,摄影,打卡", "通用"},
                {"是怎么给我推荐景区的?", "系统会根据你的浏览、收藏和评分喜好,为你挑选可能感兴趣的乡村好去处,逛得越多越懂你。", "推荐,猜你喜欢,为你推荐", "通用"},
                {"忘记密码怎么办?", "目前可联系管理员协助重置密码,建议注册后妥善保管账号信息。", "忘记密码,密码,重置", "通用"},
                {"语音导游怎么用?", "打开任意景区详情页,点击「语音导游」按钮即可自动分段播报讲解,支持暂停与停止。", "语音,导游,讲解,播报", "通用"}};
        int i = 1;
        for (String[] row : kb) {
            KnowledgeEntry e = new KnowledgeEntry();
            e.setQuestion(row[0]);
            e.setAnswer(row[1]);
            e.setKeywords(row[2]);
            e.setCategory(row[3]);
            e.setSortOrder(i++);
            e.setStatus(1);
            knowledgeEntryMapper.insert(e);
        }
    }

    // ==================== 识景哈希种子(预热线程会尝试替换为真实值) ====================

    private void seedImageHashes() {
        if (scenicImageHashMapper.selectCount(null) > 0) return;
        Random random = new Random(42);
        for (ScenicSpot s : scenicSpotMapper.selectList(null)) {
            ScenicImageHash h = new ScenicImageHash();
            h.setScenicSpotId(s.getId());
            h.setImageUrl(s.getCoverImage());
            h.setHashValue(String.format("%016x", random.nextLong()));
            scenicImageHashMapper.insert(h);
        }
    }

    // ==================== 近30天模拟浏览记录(驱动数据分析演示) ====================

    private void seedBrowseHistory() {
        if (browseHistoryMapper.selectCount(null) > 0) return;
        List<User> users = userMapper.selectList(new QueryWrapper<User>().eq("role", "USER"));
        List<ScenicSpot> spots = scenicSpotMapper.selectList(null);
        if (users.isEmpty() || spots.isEmpty()) return;
        Random random = new Random(2026);
        // 加权时段:上午/下午/晚上为高峰
        int[] hourPool = {8, 9, 9, 10, 10, 10, 11, 11, 13, 14, 14, 15, 15, 15, 16, 16, 19, 20, 20, 20, 21, 21};
        for (int i = 0; i < 300; i++) {
            BrowseHistory h = new BrowseHistory();
            h.setUserId(users.get(random.nextInt(users.size())).getId());
            h.setScenicSpotId(spots.get(random.nextInt(spots.size())).getId());
            h.setDuration(30 + random.nextInt(300));
            int daysAgo = random.nextInt(30);
            int hour = hourPool[random.nextInt(hourPool.length)];
            LocalDateTime t = LocalDateTime.now().minusDays(daysAgo)
                    .withHour(hour).withMinute(random.nextInt(60)).withSecond(random.nextInt(60));
            if (t.isAfter(LocalDateTime.now())) t = t.minusHours(12);
            h.setCreatedAt(t);
            browseHistoryMapper.insert(h);
        }
    }

    // ==================== 重算全部景区综合评分 ====================

    private void refreshAllScores() {
        for (ScenicSpot s : scenicSpotMapper.selectList(null)) {
            ratingService.refreshSpotScore(s.getId());
        }
    }
}
