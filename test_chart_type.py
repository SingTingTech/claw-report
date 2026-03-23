#!/usr/bin/env python3
"""测试图表类型传递"""
import asyncio
from playwright.async_api import async_playwright

BASE_URL = "http://127.0.0.1:5173"

async def test_chart_type():
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        context = await browser.new_context()
        page = await context.new_page()
        
        try:
            # 1. 先访问登录页，获取cookie
            print("1. 访问登录页...")
            await page.goto(f"{BASE_URL}/login", wait_until="domcontentloaded", timeout=30000)
            await asyncio.sleep(2)
            print(f"   URL: {page.url}")
            
            # 2. 登录
            print("2. 登录...")
            await page.fill('input[placeholder*="用户"], input[placeholder*="username"]', 'admin')
            await page.fill('input[type="password"]', 'admin123')
            await page.click('button:has-text("登 录"), button:has-text("登录")')
            await asyncio.sleep(5)
            print(f"   URL after login: {page.url}")
            
            # 3. 直接导航到报表页面
            print("3. 导航到报表页面...")
            await page.goto(f"{BASE_URL}/reports", wait_until="domcontentloaded", timeout=30000)
            await asyncio.sleep(5)
            print(f"   URL: {page.url}")
            
            # 4. 打印页面内容
            print("4. 页面内容:")
            content = await page.content()
            # 查找表格相关的HTML
            if "el-table" in content:
                print("   页面包含 el-table 组件")
            else:
                print("   页面不包含 el-table 组件")
            
            # 5. 等待任意加载完成
            await asyncio.sleep(3)
            
            # 截图
            await page.screenshot(path="/root/report-hub/test_result.png", full_page=True)
            print("5. 截图已保存: /root/report-hub/test_result.png")
            
            # 打印body的部分内容
            body = await page.locator("body").inner_text()
            print(f"6. 页面文本内容前500字符: {body[:500]}")
            
        except Exception as e:
            print(f"测试异常: {e}")
            import traceback
            traceback.print_exc()
            await page.screenshot(path="/root/report-hub/test_error.png")
        finally:
            await browser.close()

if __name__ == "__main__":
    asyncio.run(test_chart_type())
