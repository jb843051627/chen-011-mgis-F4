package com.fc.v2.controller.admin;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.log.Log;
import com.fc.v2.common.scope.ScopeLedger;
import com.fc.v2.common.scope.ScopeRegisterException;
import com.fc.v2.model.custom.ScopeCardView;
import com.fc.v2.model.custom.ScopeRegisterRequest;
import com.fc.v2.model.custom.ScopeRegisterResult;
import com.fc.v2.service.ITMgisScopeCardService;
import com.fc.v2.shiro.util.ShiroUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * 作业范围标签建档卡 Controller（新门脸，路径 {@code /mgisScope}）。
 *
 * <p>进门只认「登记」一处：添卡只有 {@code POST /mgisScope/register}。
 * 登记成个什么样子页面不许预填硬数——卡号系统给号、层级档系统折算、创建人
 * 取登录态、新增类别数系统比对，页面一样都不代填；灵不灵以服务回传为凭，
 * 页面只按回话刷新；出处单号一径由人工填写；校验的活儿全在服务层，不落进
 * 页面。旧自动生成门脸 {@code /MgisScopeCardController} 的写入门已封，
 * 扩功能走的是本控制器这处新重载。</p>
 *
 * <p>属地的人打开列表只看得见本市的单位与卡（regionCity 传本市），省局看
 * 得见全省（regionCity 空着），同一条卡两头点开是同一笔账。</p>
 *
 * @author fuce
 * @date 2026-09-27
 */
@Api(value = "作业范围标签建档卡")
@Controller
@RequestMapping("/mgisScope")
public class MgisScopeController extends BaseController {

    @Autowired
    private ITMgisScopeCardService mgisScopeCardService;

    @Log(title = "作业范围卡登记立卡", action = "register")
    @ApiOperation(value = "登记立卡", notes = "唯一进门。按单位编号对档案(同名不作数)；注销档案不收新卡；"
            + "同单位同段范围第二张顶回(抄送/自报同一件并到原卡)；挂用类别数负/零/非数/超三档顶线"
            + "指栏顶回；类别只许现行末级，混挂大类、挂废止旧类不给落；卡号系统给号，层级档系统折算")
    @PostMapping("/register")
    @RequiresPermissions("mgisScope:register")
    @ResponseBody
    public AjaxResult register(@RequestBody ScopeRegisterRequest req) {
        try {
            // 创建人按下立卡那一下自登录态取，请求里带了也不算
            return retobject(200, mgisScopeCardService.register(req, ShiroUtils.getLoginName()));
        } catch (ScopeRegisterException | IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @Log(title = "作业范围卡列表", action = "list")
    @ApiOperation(value = "卡列表", notes = "regionCity 空=省局看全省；传市名=属地只看本市。同一条卡两头同一笔账")
    @GetMapping("/list")
    @RequiresPermissions("mgisScope:list")
    @ResponseBody
    public AjaxResult list(String regionCity, String unitNo) {
        try {
            List<ScopeCardView> views = mgisScopeCardService.listViews(regionCity, unitNo);
            return retobject(200, views);
        } catch (ScopeRegisterException | IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @ApiOperation(value = "卡翻账", notes = "抬头、末级类别、出处、经手链拉齐；层级档按最新档案当场重折")
    @GetMapping("/{id}")
    @RequiresPermissions("mgisScope:list")
    @ResponseBody
    public AjaxResult view(@PathVariable("id") Long id) {
        try {
            return retobject(200, mgisScopeCardService.view(id));
        } catch (ScopeRegisterException | IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @Log(title = "作业范围卡过手续", action = "handle")
    @ApiOperation(value = "核对/定档", notes = "每过一道手续记一条经手，只顺走、只认头一笔；注销档案的老卡冻结")
    @PostMapping("/handle")
    @RequiresPermissions("mgisScope:handle")
    @ResponseBody
    public AjaxResult handle(Long cardId, String action, String note) {
        try {
            ScopeCardView view = mgisScopeCardService.handle(
                    cardId, action, ShiroUtils.getLoginName(), note);
            return retobject(200, view);
        } catch (ScopeRegisterException | IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @ApiOperation(value = "看账对账屏", notes = "省局按新增类别最多的单位往下排、属地按最末一级归堆，"
            + "两途卡数当场对，差一笔这一屏不算过")
    @GetMapping("/ledger")
    @RequiresPermissions("mgisScope:list")
    @ResponseBody
    public AjaxResult ledger(String regionCity) {
        try {
            ScopeLedger ledger = mgisScopeCardService.ledger(regionCity);
            return retobject(200, ledger);
        } catch (ScopeRegisterException | IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @ApiOperation(value = "现行专业类别目录", notes = "大类与末级一棵树；卡上只许挂末级；废止类别不在此列")
    @GetMapping("/catalog")
    @RequiresPermissions("mgisScope:list")
    @ResponseBody
    public AjaxResult catalog() {
        return retobject(200, mgisScopeCardService.currentCatalog());
    }
}
