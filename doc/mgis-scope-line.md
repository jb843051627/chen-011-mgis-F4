# 作业范围标签建档卡规矩 → 系统口径对照

资质监督处把去年九月省局联查暴露的问题（合同写「界线与不动产测绘」，作业范围
记录里只找得到「工程测量」一项，其余全是空白）定成系统里的一张张卡。规矩在
系统里只有一处出处：`com.fc.v2.common.scope.ScopeCardJudge`（立卡推演）。
登记门、翻账列表、卡页面、看账对账屏全从这一处过——两头数出来的卡数合不上
便是事故，所以别处不许另写一套。

一张卡只讲一段范围；卡上没有的类别，业务就办不成。卡号由系统给号，页面不预
填硬数，灵不灵以服务回传为凭。

| 处里的说法 | 系统口径 | 落点 |
| --- | --- | --- |
| 卡号系统给号 | 卡号 `FW+雪花` 由服务层写；请求带卡号、层级档、创建人、新增数一律不认。同一单位同一段范围想再立一张，第二张顶回去，回的是头一笔（duplicated） | `TMgisScopeCardServiceImpl.register`、唯一键 `uk_scope_card_scope` |
| 抄送与自报是同一件事 | 出处分 `CITY_COPY` 市局抄送、`SELF_REPORT` 单位自报两类，并在同一张卡上：同单位同范围签名第二回带新出处来的，出处并进原卡，不立新卡，账上不许留两笔；同一请求同类出处两条也顶回 | `t_mgis_scope_source` `uk_scope_source_type`、`TMgisScopeCardServiceImpl.mergeSources` |
| 负数/零/超顶线，说清拦在哪一栏 | 挂用类别数只认正整数：空白、写字、带小数→「不是数」，负数→「不得为负」，零→「不得为零」，越过档案三档顶线→「超出顶线」，全部指到「挂用类别数」栏，浑数带填报原值；只甩一句「不合法」等于没说 | `ScopeCardJudge.parseScopeCount`、`checkWithinCeiling`、`ScopeField.SCOPE_COUNT` |
| 数与卡上类别须是同一段范围 | 挂用类别数必须等于卡上实挂末级类别条数，两数对不上同样拦在「挂用类别数」栏 | `ScopeCardJudge.checkCountMatches` |
| 卡必须有出处 | 立卡先按**资质单位编号**对基础档案；只按名称匹配不作数（同名两家我市真有过）。档案显示已注销的，往后不再收新卡；老卡照旧查得到，但不能从老卡上接着添东西（过手续也冻结） | `TMgisScopeCardServiceImpl.register/handle`、`t_mgis_unit_doc.status` |
| 类别按现行目录走成一棵树 | 一级是大类（G01..G10），二级才是具体作业类别（末级），卡上只许挂末级；把大类与末级混挂在一张卡上不给落；挂一个早已并进别处的旧类别（REVOKED）也不给落，回话指它并到哪个现行类别 | `ScopeCardJudge.normalizeCategories`、`t_mgis_prof_category` |
| 层级档不许人手填 | 系统拿挂用类别数去撞档案上的三道顶线折算：一档 `<th1`，恰为一档顶线按二档说话，恰为二档顶线按三档说话（等号算高一档），恰为三档顶线归三档不设四档；系数=挂用类别数÷所在档顶线，四舍五入留两位小数 | `ScopeCardJudge.grade` |
| 只折一次远远不够 | 类别有增减、单位改了属地，层级档翻账时拿**最新档案**当场重新折算；卡上立卡那日折的一份只留作快照，页面两份并列（卡上一份、当前一份） | `ScopeCardJudge.regrade`、`TMgisScopeCardServiceImpl.toView` |
| 新增类别系统比对 | 本年度新增挂用类别数 = 本卡末级类别里该单位老卡上从没挂过的条数，立卡当场按单位并集算出，不劳手填 | `ScopeCardJudge.newCategoryCount` |
| 创建人按下立卡那一下 | `create_by` 由服务层按登录态写死；后来人把整张卡重录一遍也不许挪成自己 | `TMgisScopeCard`、`register` |
| 经手人各记各的 | 每过一道手续（立卡/核对/定档）在经手链记一条，只增不改；经手链与创建人是两个位置，不许互相顶；同一道手续第二回来只认头一笔（哪怕卡已办到后一道）；手续只许一道接一道顺走 | `t_mgis_scope_handler`、`TMgisScopeCardServiceImpl.handle` |
| 两头看账数必须一致 | 省局从新增类别最多的单位往下排（按单位归堆、新增数相加），属地把单位按最末一级（区县）归堆再数；两条路数出来的卡数必须都等于行目总数，差一笔这一屏没过 | `ScopeCardJudge.ledger`、`/mgisScope/ledger` |
| 属地只见本市 | 属地列表按档案 `region_city` 过滤，只看得见本市的单位与卡；省局（市名空着）看得见全省；同一条卡两边点开是同一笔账 | `TMgisScopeCardServiceImpl.listViews` |

## 门脸规矩

- **进门只认「登记」一处**：添卡只有 `POST /mgisScope/register`，卡号、层级档、创建人、新增数页面一律不许预填，校验的活儿全在服务层，不落进页面；灵不灵以接口回传为凭，页面只按回话刷新。
- **出处单号一径人工填写**：来文/自报单号系统不代拟，空着指「出处单号」栏顶回。
- **原有签名照封**：2026-09-12 自动生成的 `/MgisScopeCardController` 与服务接口六个旧方法签名一仍其旧；旧 add/edit/remove 三个写入门已焊死（回明白话，不落账），读台账照旧；新意思全走 `/mgisScope` 这处新重载。
- **卡只立不改**：`t_mgis_scope_card` 不设 update_by/update_time/del_flag；卡上内容无改、删入口，只有手续状态由 `/mgisScope/handle` 顺着推进。明细、出处、经手链三张表只增不改。

## 立卡被顶回时指向的栏目（`ScopeField`）

| 栏 | 触发 |
| --- | --- |
| `UNIT_NO` 单位编号 | 空、对不上在册档案、档案已注销（名称再像不作数） |
| `SCOPE_COUNT` 挂用类别数 | 空、不是数、负数、零、超三档顶线、与实挂末级条数不符（异常带填报原值） |
| `CATEGORIES` 挂用类别 | 一栏空白；条目中夹空白代号 |
| `CAT_CODE` 类别代号 | 不识、册内重复、挂的是大类（混挂）、挂已废止并入别处的旧类别 |
| `SOURCE_TYPE` 出处 | 一条出处都没有、类型不识、同类出处一卡两条 |
| `SOURCE_DOC_NO` 出处单号 | 来文/自报单号空白（单号人工填） |
| `HANDLE_ACTION` 卡上手续 | 手续不识、隔站落笔、注销档案老卡上添手续、并发同办一道 |

## 看账对账（`ScopeLedger`）

| 数 | 口径 |
| --- | --- |
| total 卡总数 | 视角内行目总数（省局全省 / 属地带本市） |
| unitRanking | 一单位一行：卡数、本年度新增类别合计；新增多的在前，并列按单位编号 |
| regionBuckets | 按最末一级（区县）归堆：堆内单位数、卡数；属地按区县名排序 |

恒等校验：按单位小计之和 = 按区县归堆小计之和 = total。任一对不上即
`ScopeCardJudge.ledger` 抛账，这一屏不算过。

## 端点（新门脸 `/mgisScope`）

| 端点 | 用途 |
| --- | --- |
| `POST /mgisScope/register` | 登记立卡（唯一写卡门；权限 `mgisScope:register`）。重卡顶回并回头一笔，duplicated 为真 |
| `GET  /mgisScope/list` | 卡列表（权限 `mgisScope:list`）：regionCity 空=省局全省，传市名=属地本市；可再按 unitNo 收窄 |
| `GET  /mgisScope/{id}` | 卡翻账：抬头、末级类别、出处、经手链一次拉齐，层级档按最新档案当场重折 |
| `POST /mgisScope/handle` | 核对/定档（权限 `mgisScope:handle`），每道一条经手，只顺走、只认头一笔 |
| `GET  /mgisScope/ledger` | 看账对账屏（权限 `mgisScope:list`）：两头卡数当场对 |
| `GET  /mgisScope/catalog` | 现行专业类别目录（大类/末级一棵树；废止类别不在此列） |

旧门 `/MgisScopeCardController`：list/view 照旧，add/edit/remove 已焊死。

建表脚本：`sql/mgis_scope.sql`（专业类别目录树、范围卡、类别明细、出处、
经手链五表；基础档案补省/市/区县三列；十类大类末级目录与两条废止旧类别种子，
旧库补列语句以注释给出）。
