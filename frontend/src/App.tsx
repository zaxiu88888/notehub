import { Route, Routes } from 'react-router-dom';
import UserLayout from './apps/user/layout/UserLayout';
import HomePage from './apps/user/pages/home/HomePage';
import QuestionListPage from './apps/user/pages/questionList/QuestionListPage';
import NotFound from './base/components/NotFound';

/**
 * 路由(渐进落地): M5 批次 B/C 会逐步加入题目详情/个人中心/消息
 */
export default function App() {
  return (
    <Routes>
      <Route path="/" element={<UserLayout />}>
        <Route index element={<HomePage />} />
        <Route path="question-lists" element={<QuestionListPage />} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}
