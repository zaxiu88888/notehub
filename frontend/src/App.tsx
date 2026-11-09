import { Route, Routes } from 'react-router-dom';
import UserLayout from './apps/user/layout/UserLayout';
import HomePage from './apps/user/pages/home/HomePage';
import QuestionPage from './apps/user/pages/question/QuestionPage';
import QuestionListPage from './apps/user/pages/questionList/QuestionListPage';
import UserCenterPage from './apps/user/pages/userCenter/UserCenterPage';
import UserHomePage from './apps/user/pages/userHome/UserHomePage';
import MessagePage from './apps/user/pages/message/MessagePage';
import AdminLayout from './apps/admin/AdminLayout';
import DashboardPage from './apps/admin/pages/dashboard/DashboardPage';
import QuestionsAdminPage from './apps/admin/pages/questions/QuestionsAdminPage';
import CategoriesAdminPage from './apps/admin/pages/categories/CategoriesAdminPage';
import QuestionListsAdminPage from './apps/admin/pages/questionLists/QuestionListsAdminPage';
import UsersAdminPage from './apps/admin/pages/users/UsersAdminPage';
import NotFound from './base/components/NotFound';

/** 用户端 + 管理端路由; 管理端由 AdminLayout 做 admin 守卫 */
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

      <Route path="/admin" element={<AdminLayout />}>
        <Route index element={<DashboardPage />} />
        <Route path="questions" element={<QuestionsAdminPage />} />
        <Route path="categories" element={<CategoriesAdminPage />} />
        <Route path="question-lists" element={<QuestionListsAdminPage />} />
        <Route path="users" element={<UsersAdminPage />} />
      </Route>
    </Routes>
  );
}
