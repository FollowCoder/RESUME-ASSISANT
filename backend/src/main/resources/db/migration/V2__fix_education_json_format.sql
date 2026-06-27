-- ============================================================
-- 修复 education 字段中存储为对象而非数组的旧数据
-- 将 {"degree":"xxx","school":"xxx","major":"xxx"} 转换为 [{"degree":"xxx","school":"xxx","major":"xxx"}]
-- ============================================================

UPDATE user_profiles
SET education = jsonb_build_array(education)
WHERE education IS NOT NULL
  AND jsonb_typeof(education) = 'object';
