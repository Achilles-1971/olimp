package com.example.olimp.ui.friends

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.olimp.R
import com.example.olimp.data.models.Friendship
import com.example.olimp.data.models.UserResponse
import com.example.olimp.data.repository.UserRepository
import com.example.olimp.databinding.ActivityFindFriendsBinding
import com.example.olimp.network.RetrofitInstance
import com.example.olimp.ui.ProfileHostActivity
import com.example.olimp.utils.SessionManager
import kotlinx.coroutines.launch

class FindFriendsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFindFriendsBinding
    private lateinit var adapter: FindFriendsAdapter

    private var allUsers = listOf<UserResponse>()
    private var friendships = listOf<Friendship>()
    private var currentUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFindFriendsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        currentUserId = SessionManager(this).getUserId() ?: -1

        adapter = FindFriendsAdapter(
            currentUserId = currentUserId,
            friendships = emptyList(),
            onAddClick = { user -> sendFriendRequest(user) },
            onCancelClick = { user -> cancelFriendRequest(user) },
            onAcceptClick = { user -> acceptFriendRequest(user) },
            onDeclineClick = { user -> declineFriendRequest(user) },
            onAvatarClick = { user -> navigateToProfile(user) } // Обработка клика по аватарке
        )

        binding.rvUsers.layoutManager = LinearLayoutManager(this)
        binding.rvUsers.adapter = adapter

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterUsers(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.swipeRefreshLayout.setOnRefreshListener {
            loadUsers()
        }

        loadUsers()
    }

    private fun loadUsers() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val repository = UserRepository(RetrofitInstance.getApi(this@FindFriendsActivity))

                val usersResponse = repository.getAllUsers()
                val friendshipsResponse = repository.getFriendships()

                if (usersResponse.isSuccessful && friendshipsResponse.isSuccessful) {
                    allUsers = usersResponse.body()?.filter { it.id != currentUserId } ?: emptyList()
                    friendships = friendshipsResponse.body() ?: emptyList()

                    adapter.updateFriendships(friendships)
                    adapter.submitList(allUsers)

                    filterUsers(binding.etSearch.text.toString())
                } else {
                    val errorMsg = "Ошибка загрузки: ${usersResponse.code()} / ${friendshipsResponse.code()}"
                    Toast.makeText(this@FindFriendsActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@FindFriendsActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefreshLayout.isRefreshing = false
                binding.progressBar.visibility = View.GONE
                toggleEmptyText()
            }
        }
    }

    private fun filterUsers(query: String) {
        val filtered = allUsers.filter {
            it.username.contains(query, ignoreCase = true)
        }
        adapter.submitList(filtered)
        toggleEmptyText(filtered)
    }

    private fun toggleEmptyText(filteredList: List<UserResponse>? = null) {
        val listToCheck = filteredList ?: allUsers
        binding.tvEmpty.visibility = if (listToCheck.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun sendFriendRequest(user: UserResponse) {
        lifecycleScope.launch {
            try {
                val repository = UserRepository(RetrofitInstance.getApi(this@FindFriendsActivity))
                val response = repository.sendFriendRequest(user.id)
                if (response.isSuccessful) {
                    Toast.makeText(this@FindFriendsActivity, "Заявка отправлена", Toast.LENGTH_SHORT).show()
                    loadUsers()
                } else {
                    Toast.makeText(this@FindFriendsActivity, "Ошибка: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@FindFriendsActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cancelFriendRequest(user: UserResponse) {
        lifecycleScope.launch {
            try {
                val repository = UserRepository(RetrofitInstance.getApi(this@FindFriendsActivity))
                val response = repository.cancelFriendRequest(user.id)
                if (response.isSuccessful) {
                    Toast.makeText(this@FindFriendsActivity, "Заявка отменена", Toast.LENGTH_SHORT).show()
                    loadUsers()
                } else {
                    Toast.makeText(this@FindFriendsActivity, "Ошибка: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@FindFriendsActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun acceptFriendRequest(user: UserResponse) {
        val friendship = friendships.find {
            it.user.id == user.id || it.friend.id == user.id
        }
        val friendshipId = friendship?.id ?: return

        lifecycleScope.launch {
            try {
                val repository = UserRepository(RetrofitInstance.getApi(this@FindFriendsActivity))
                val response = repository.acceptFriendRequest(friendshipId)
                if (response.isSuccessful) {
                    Toast.makeText(this@FindFriendsActivity, "Заявка принята", Toast.LENGTH_SHORT).show()
                    loadUsers()
                } else {
                    Toast.makeText(this@FindFriendsActivity, "Ошибка: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@FindFriendsActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun declineFriendRequest(user: UserResponse) {
        lifecycleScope.launch {
            try {
                val repository = UserRepository(RetrofitInstance.getApi(this@FindFriendsActivity))
                val response = repository.cancelFriendRequest(user.id)
                if (response.isSuccessful) {
                    Toast.makeText(this@FindFriendsActivity, "Заявка отклонена", Toast.LENGTH_SHORT).show()
                    loadUsers()
                } else {
                    Toast.makeText(this@FindFriendsActivity, "Ошибка: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@FindFriendsActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Переход на экран профиля другого пользователя через ProfileHostActivity
    private fun navigateToProfile(user: UserResponse) {
        val intent = Intent(this, ProfileHostActivity::class.java)
        intent.putExtra("userId", user.id)
        startActivity(intent)
    }
}
