import { Route, Routes } from 'react-router-dom';
import UserLayout from './apps/user/layout/UserLayout';
import HomePage from './apps/user/pages/home/HomePage';
import QuestionPage from './apps/user/pages/question/QuestionPage';
import QuestionListPage from './apps/user/pages/questionList/QuestionListPage';
import UserCenterPage from './apps/user/pages/userCenter/UserCenterPage';
import UserHomePage from './apps/user/pages/userHome/UserHomePage';
import MessagePage from './apps/user/pages/message/MessagePage';
import NotFound from './base/components/NotFound';

/** 用户端路由(管理端 /admin 在 M6 挂载) */
export default function App() {
  return (
    <Routes>
      <Route path="/" element={<UserLayout />}>
        <Route index element={<HomePage />} />
        <Route path="questions/:questionId" element={<QuestionPage />} />
        <Route path="question-lists" element={<QuestionListPage />} />
        <Route path="user-center" element={<UserCenterPage />} />
        <Route path="users/:userId" element={<UserHomePage />} />
        <Route path="messages" element={<MessagePage />} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}
