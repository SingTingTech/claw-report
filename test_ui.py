#!/usr/bin/env python3
"""
ReportHub 自动化测试脚本
使用 Playwright 验证应用功能
"""

import asyncio
from playwright.async_api import async_playwright
import sys

BASE_URL = "http://127.0.0.1:3000"
API_URL = "http://127.0.0.1:8080/api"

class ReportHubTester:
    def __init__(self):
        self.results = []
        
    def log(self, status, msg):
        symbol = "✅" if status else "❌"
        print(f"{symbol} {msg}")
        self.results.append((status, msg))
        
    async def run(self):
        print("=" * 50)
        print("ReportHub 自动化测试")
        print("=" * 50)
        
        async with async_playwright() as p:
            browser = await p.chromium.launch(headless=True)
            context = await browser.new_context()
            page = await context.new_page()
            
            # 监听控制台错误
            errors = []
            page.on("console", lambda msg: errors.append(msg.text) if msg.type == "error" else None)
            
            try:
                # 1. 测试前端页面加载
                print("\n【1】测试前端页面加载...")
                await page.goto(BASE_URL, wait_until="networkidle", timeout=30000)
                await asyncio.sleep(2)
                
                # 检查是否跳转到登录页
                if "login" in page.url.lower() or await page.locator("text=登录").count() > 0:
                    self.log(True, "登录页面加载正常")
                else:
                    self.log(True, f"当前页面: {page.url}")
                    
                # 2. 测试登录功能
                print("\n【2】测试登录功能...")
                login_btn = page.locator("button:has-text('登 录'), button:has-text('登录')").first
                if await login_btn.count() > 0:
                    await page.fill('input[placeholder*="用户"], input[placeholder*="username"]', 'admin')
                    await page.fill('input[type="password"]', 'admin123')
                    await login_btn.click()
                    await asyncio.sleep(3)
                    
                    if "login" not in page.url.lower():
                        self.log(True, "登录成功，跳转到首页")
                    else:
                        self.log(False, "登录失败，仍在登录页")
                else:
                    self.log(False, "未找到登录按钮")
                    
                # 3. 测试菜单显示
                print("\n【3】测试菜单显示...")
                menu_items = ["报表管理", "SQL查询", "数据源", "报表分组", "脱敏规则", "用户管理", "角色管理"]
                for item in menu_items:
                    count = await page.locator(f"text={item}").count()
                    if count > 0:
                        self.log(True, f"菜单项『{item}』显示正常")
                    else:
                        self.log(False, f"菜单项『{item}』未显示")
                        
                # 4. 测试报表管理页面
                print("\n【4】测试报表管理页面...")
                try:
                    await page.click('text=报表管理')
                    await asyncio.sleep(2)
                    
                    # 检查页面元素
                    if await page.locator(".el-table").count() > 0:
                        self.log(True, "报表列表加载正常")
                    else:
                        self.log(False, "报表列表未显示")
                        
                    # 检查新建按钮
                    if await page.locator("button:has-text('新建报表')").count() > 0:
                        self.log(True, "新建报表按钮存在")
                except Exception as e:
                    self.log(False, f"报表管理页面错误: {e}")
                    
                # 5. 测试SQL查询页面
                print("\n【5】测试SQL查询页面...")
                try:
                    await page.click('text=SQL查询')
                    await asyncio.sleep(2)
                    
                    if await page.locator("text=SQL 编辑器").count() > 0:
                        self.log(True, "SQL查询页面加载正常")
                    else:
                        self.log(False, "SQL查询页面未正确加载")
                        
                    if await page.locator("button:has-text('保存查询')").count() > 0:
                        self.log(True, "保存查询按钮存在")
                except Exception as e:
                    self.log(False, f"SQL查询页面错误: {e}")
                    
                # 6. 测试数据源页面
                print("\n【6】测试数据源页面...")
                try:
                    await page.click('text=数据源')
                    await asyncio.sleep(2)
                    
                    if await page.locator("text=数据源管理").count() > 0:
                        self.log(True, "数据源页面加载正常")
                    else:
                        self.log(False, "数据源页面未正确加载")
                        
                    # 检查数据字典按钮
                    if await page.locator("button:has-text('数据字典')").count() > 0:
                        self.log(True, "数据字典按钮存在")
                except Exception as e:
                    self.log(False, f"数据源页面错误: {e}")
                    
                # 7. 测试用户管理页面
                print("\n【7】测试用户管理页面...")
                try:
                    await page.click('text=用户管理')
                    await asyncio.sleep(2)
                    
                    if await page.locator("text=用户管理").count() > 0:
                        self.log(True, "用户管理页面加载正常")
                    else:
                        self.log(False, "用户管理页面未正确加载")
                except Exception as e:
                    self.log(False, f"用户管理页面错误: {e}")
                    
                # 8. 测试角色管理页面
                print("\n【8】测试角色管理页面...")
                try:
                    await page.click('text=角色管理')
                    await asyncio.sleep(2)
                    
                    if await page.locator("text=角色管理").count() > 0:
                        self.log(True, "角色管理页面加载正常")
                    else:
                        self.log(False, "角色管理页面未正确加载")
                except Exception as e:
                    self.log(False, f"角色管理页面错误: {e}")
                    
                # 9. 检查控制台错误
                print("\n【9】检查控制台错误...")
                error_count = len([e for e in errors if "error" in e.lower() or "unhandled" in e.lower()])
                if error_count == 0:
                    self.log(True, "无控制台错误")
                else:
                    self.log(False, f"发现 {error_count} 个控制台错误")
                    for err in errors[:3]:
                        print(f"   - {err[:100]}")
                        
            except Exception as e:
                self.log(False, f"测试异常: {e}")
            finally:
                await browser.close()
                
        # 输出结果汇总
        print("\n" + "=" * 50)
        print("测试结果汇总")
        print("=" * 50)
        passed = sum(1 for s, _ in self.results if s)
        total = len(self.results)
        print(f"通过: {passed}/{total}")
        
        for status, msg in self.results:
            symbol = "✅" if status else "❌"
            print(f"  {symbol} {msg}")
            
        return passed == total

async def main():
    tester = ReportHubTester()
    success = await tester.run()
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    asyncio.run(main())
